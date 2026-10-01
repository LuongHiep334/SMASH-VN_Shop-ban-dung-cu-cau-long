const fs = require('fs');
const path = require('path');
const https = require('https');

const MCP_URL = process.env.MCP_URL;
const MCP_TOKEN = process.env.MCP_TOKEN;

if (!MCP_URL || !MCP_TOKEN) {
    console.error("Please set MCP_URL and MCP_TOKEN before running deploy.js.");
    process.exit(1);
}

// Hàm gửi request HTTP
function sendRequest(payload, sessionId = null) {
    return new Promise((resolve, reject) => {
        const url = new URL(MCP_URL);
        const headers = {
            "Authorization": `Bearer ${MCP_TOKEN}`,
            "Content-Type": "application/json",
            "Accept": "application/json, text/event-stream"
        };
        if (sessionId) {
            headers["Mcp-Session-Id"] = sessionId;
        }

        const options = {
            hostname: url.hostname,
            port: url.port,
            path: url.pathname,
            method: 'POST',
            headers: headers,
        };

        const req = https.request(options, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk.toString());
            res.on('end', () => {
                resolve({
                    headers: res.headers,
                    body: body
                });
            });
        });

        req.on('error', (e) => reject(e));
        req.write(payload);
        req.end();
    });
}

// Gọi một Tool qua MCP
async function callMcp(toolName, arguments = {}, sessionId) {
    const payload = JSON.stringify({
        jsonrpc: "2.0",
        id: Date.now(),
        method: "tools/call",
        params: {
            name: toolName,
            arguments: arguments
        }
    });

    const res = await sendRequest(payload, sessionId);
    let body = res.body.trim();

    // Parse SSE format (data: {...})
    if (body.includes('data:')) {
        const lines = body.split('\n');
        for (let line of lines) {
            if (line.startsWith('data:')) {
                body = line.substring(5).trim();
                break;
            }
        }
    }

    return JSON.parse(body);
}

// Lấy Session ID
async function getMcpSession() {
    const initPayload = JSON.stringify({
        jsonrpc: "2.0",
        id: 1,
        method: "initialize",
        params: {
            protocolVersion: "2024-11-05",
            capabilities: {},
            clientInfo: { name: "NodeDeployClient", version: "1.0.0" }
        }
    });

    const res = await sendRequest(initPayload);

    // In ra để kiểm tra
    console.log("==== HEADERS ====");
    console.log(res.headers);

    console.log("==== BODY ====");
    console.log(res.body);

    // Lấy Session ID từ Headers
    const sessionId = res.headers['mcp-session-id'];

    if (sessionId) {
        const initializedPayload = JSON.stringify({
            jsonrpc: "2.0",
            method: "notifications/initialized",
            params: {}
        });

        await sendRequest(initializedPayload, sessionId);
    }

    return sessionId;
}

// Tìm file .jar trong thư mục target
function findJarFile() {
    const targetDir = path.join(__dirname, 'target');
    if (!fs.existsSync(targetDir)) return null;

    const files = fs.readdirSync(targetDir);
    const jarFile = files.find(f => f.endsWith('.jar') && !f.endsWith('-plain.jar'));

    return jarFile ? path.join(targetDir, jarFile) : null;
}

async function deploy() {
    console.log("🚀 Bắt đầu quá trình Deploy qua MCP...");

    const jarPath = findJarFile();
    if (!jarPath) {
        console.error("❌ Không tìm thấy file .jar trong thư mục 'target'. Bạn đã chạy lệnh 'mvnw clean package' chưa?");
        process.exit(1);
    }
    console.log(`📦 Đã tìm thấy file build: ${path.basename(jarPath)}`);

    try {
        console.log("🔗 Đang kết nối tới MCP Server...");
        const sessionId = await getMcpSession();
        if (!sessionId) {
            throw new Error("Không thể lấy Session ID từ máy chủ MCP. Kiểm tra lại Token.");
        }
        console.log(`✅ Kết nối thành công! Session ID: ${sessionId}`);

        // Đọc file .jar thành base64 string
        const jarContentBase64 = fs.readFileSync(jarPath, 'base64');
        const jarName = path.basename(jarPath);

        console.log(`⬆️ Đang upload dữ liệu dạng base64... (việc này có thể mất vài phút do file .jar khá lớn)`);

        // 1. Ghi file .b64 lên server
        const b64WriteRes = await callMcp("write_file", {
            path: `public_html/${jarName}.b64`,
            content: jarContentBase64
        }, sessionId);

        if (b64WriteRes && b64WriteRes.result && !b64WriteRes.error) {
            console.log("✅ Đã upload file base64 thành công.");
        } else {
            console.error("❌ Lỗi khi upload file base64:", b64WriteRes.error || b64WriteRes);
            return;
        }

        // 2. Tạo một file PHP nhỏ trên server để giải mã base64 thành file nhị phân .jar
        console.log("⚙️ Đang tạo script giải mã trên server...");
        const phpDecoderCode = `<?php
        $b64File = __DIR__ . '/${jarName}.b64';
        $jarFile = __DIR__ . '/${jarName}';
        if (file_exists($b64File)) {
            $data = base64_decode(file_get_contents($b64File));
            file_put_contents($jarFile, $data);
            unlink($b64File); // Xóa file b64
            unlink(__FILE__); // Xóa chính script này
            echo "OK: Đã giải mã thành công file ${jarName}!";
        } else {
            echo "ERROR: Không tìm thấy file b64.";
        }
        ?>`;

        const phpWriteRes = await callMcp("write_file", {
            path: "public_html/deploy_decoder.php",
            content: phpDecoderCode
        }, sessionId);

        if (phpWriteRes && phpWriteRes.result && !phpWriteRes.error) {
            console.log("🎉 Upload hoàn tất!");
            console.log("==========================================");
            console.log("⚠️ BƯỚC CUỐI CÙNG (QUAN TRỌNG):");
            console.log("File .jar là file nhị phân nên hệ thống đã bọc nó qua định dạng base64.");
            console.log("Để giải mã và hoàn tất, bạn hãy mở trình duyệt và truy cập vào đường link sau:");
            console.log("👉 http://TEN_MIEN_CUA_BAN/deploy_decoder.php");
            console.log("(Ví dụ: http://schedules.id.vn/deploy_decoder.php)");
            console.log("Nếu trang báo 'OK', file .jar của bạn đã nằm trên server an toàn và script giải mã đã tự động tự hủy!");
            console.log("==========================================");
        } else {
            console.error("❌ Lỗi khi tạo file decoder:", phpWriteRes.error || phpWriteRes);
        }

    } catch (err) {
        console.error("💥 Lỗi hệ thống:", err.message);
    }
}

deploy();
