/* Run after the isolated MVC test exports HTML with -Dsupport.preview=true.
 * Uses existing workspace Puppeteer; serves rendered pages and real static assets
 * on loopback only. It has no database and rejects all non-GET requests.
 */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');

async function main() {
    const { default: puppeteer } = await import('puppeteer');
    const root = path.resolve(__dirname, '../../..');
    const output = path.join(root, 'target/support-preview');
    const assets = path.join(root, 'src/main/resources/static');
    const routes = ['/huong-dan-mua-hang', '/huong-dan-thanh-toan', '/chinh-sach'];
    const mime = { '.css': 'text/css', '.js': 'text/javascript', '.html': 'text/html', '.png': 'image/png', '.jpg': 'image/jpeg', '.svg': 'image/svg+xml', '.woff2': 'font/woff2', '.woff': 'font/woff' };
    const server = http.createServer((req, res) => {
        if (req.method !== 'GET') { res.writeHead(405).end(); return; }
        const url = new URL(req.url, 'http://localhost');
        if (url.pathname === '/api/chat/history') { res.setHeader('Content-Type', 'application/json'); res.end('[]'); return; }
        if (url.pathname === '/gio-hang/api/mini-cart') { res.setHeader('Content-Type', 'application/json'); res.end('{"trangThai":"ok","tongSoLuong":0,"tongTien":0,"danhSachCart":[]}'); return; }
        const file = routes.includes(url.pathname)
            ? path.join(output, url.pathname.substring(1) + '.html')
            : path.resolve(assets, '.' + decodeURIComponent(url.pathname));
        if (!file.startsWith(assets + path.sep) && !routes.includes(url.pathname)) { res.writeHead(403).end(); return; }
        if (!fs.existsSync(file) || !fs.statSync(file).isFile()) { res.writeHead(404).end(); return; }
        res.setHeader('Content-Type', (mime[path.extname(file)] || 'application/octet-stream') + (['.css','.js','.html'].includes(path.extname(file)) ? '; charset=utf-8' : ''));
        fs.createReadStream(file).pipe(res);
    });
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
    const base = 'http://127.0.0.1:' + server.address().port;
    const browser = await puppeteer.launch({ headless: true, args: ['--no-sandbox'], ...(process.env.SUPPORT_CHROME ? { executablePath: process.env.SUPPORT_CHROME } : {}) });
    const report = [];
    try {
        const page = await browser.newPage();
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        await page.setRequestInterception(true);
        page.on('request', request => {
            const url = request.url();
            if (request.method() !== 'GET') return request.abort();
            if (url.startsWith(base) || url.startsWith('data:') || /^https:\/\/(fonts\.googleapis\.com|fonts\.gstatic\.com|cdnjs\.cloudflare\.com|cdn\.jsdelivr\.net)\//.test(url)) return request.continue();
            return request.abort();
        });
        for (const width of [1440, 390, 320]) {
            await page.setViewport({ width, height: 900, isMobile: width < 500, hasTouch: width < 500 });
            for (const route of routes) {
                const response = await page.goto(base + route, { waitUntil: 'networkidle2' });
                assert.equal(response.status(), 200);
                await page.evaluate(() => document.fonts.ready);
                // Verify lazily loaded guide images after they enter the viewport.
                for (const img of await page.$$('main img')) {
                    await img.evaluate(element => element.scrollIntoView());
                    await page.waitForFunction(element => element.complete && element.naturalWidth > 0, {}, img);
                }
                await page.evaluate(() => window.scrollTo(0, 0));
                const layout = await page.evaluate(() => ({
                    width: window.innerWidth,
                    scroll: document.documentElement.scrollWidth,
                    images: [...document.querySelectorAll('main img')].every(img => img.complete && img.naturalWidth > 0),
                    h1: document.querySelector('main h1').textContent,
                    hidden: document.querySelector('.support-nav-menu').hidden
                }));
                assert.ok(layout.scroll <= layout.width + 1, `Overflow ${route} ${width}: ${JSON.stringify(layout)}`);
                assert.ok(layout.images, `Broken images: ${route}`);
                assert.ok(layout.hidden);
                await page.screenshot({ path: path.join(output, route.substring(1) + '-overview-' + width + '.png') });
                await page.screenshot({ path: path.join(output, route.substring(1) + '-' + width + '.png'), fullPage: true });
                if (width === 390 && route === routes[0]) {
                    await page.$eval('#chon-phan-loai', element => element.scrollIntoView());
                    await page.screenshot({ path: path.join(output, 'buying-step-mobile.png') });
                }
                report.push({ route, width, status: 200, overflow: false, images: 'OK' });
            }
        }
        await page.setViewport({ width: 1440, height: 900, isMobile: false, hasTouch: false });
        await page.goto(base + routes[0], { waitUntil: 'networkidle2' });
        const trigger = '.support-nav-trigger';
        const expanded = () => page.$eval(trigger, element => element.getAttribute('aria-expanded'));
        await page.hover(trigger);
        assert.equal(await expanded(), 'true', 'Mouse hover opens');
        await page.mouse.move(10, 400);
        assert.equal(await expanded(), 'false', 'Mouse leave closes');
        // Focus and activate from the keyboard, independent of pointer hover.
        await page.focus(trigger);
        await page.keyboard.press('Enter');
        assert.equal(await expanded(), 'true', 'Enter opens');
        await page.keyboard.press('Tab');
        assert.equal(await page.evaluate(() => document.activeElement.textContent), 'Hướng dẫn mua hàng');
        await page.keyboard.press('Tab');
        assert.equal(await page.evaluate(() => document.activeElement.textContent), 'Hướng dẫn thanh toán');
        await page.keyboard.press('Escape');
        assert.equal(await expanded(), 'false', 'Escape closes');
        assert.equal(await page.evaluate(() => document.activeElement.className), 'support-nav-trigger');
        await page.keyboard.press('Space');
        assert.equal(await expanded(), 'true', 'Space opens');
        await page.keyboard.press('ArrowDown');
        assert.equal(await page.evaluate(() => document.activeElement.textContent), 'Hướng dẫn mua hàng');
        await page.keyboard.press('ArrowDown');
        assert.equal(await page.evaluate(() => document.activeElement.textContent), 'Hướng dẫn thanh toán');
        await page.keyboard.press('ArrowUp');
        assert.equal(await page.evaluate(() => document.activeElement.textContent), 'Hướng dẫn mua hàng');
        await page.keyboard.press('Escape');
        await page.keyboard.press('Enter');
        await page.screenshot({ path: path.join(output, 'dropdown-desktop.png') });
        await Promise.all([page.waitForNavigation({ waitUntil: 'networkidle2' }), page.click('.support-nav-menu li:last-child a')]);
        assert.equal(new URL(page.url()).pathname, routes[1]);
        report.push({ dropdown: 'desktop', hover: 'OK', keyboard: 'OK', navigate: 'OK' });

        await page.setViewport({ width: 390, height: 844, isMobile: true, hasTouch: true });
        await page.goto(base + routes[0], { waitUntil: 'networkidle2' });
        await page.tap('#navigation2 .toggle-button');
        await page.waitForSelector('#navigation2.js-open');
        await page.waitForFunction(() => document.querySelector('#navigation2 .ah-lg-mode').getBoundingClientRect().left >= -1);
        await page.tap(trigger);
        assert.equal(await expanded(), 'true', 'Touch opens');
        assert.equal(await page.$eval('.support-nav-menu', e => getComputedStyle(e).display), 'block');
        await page.screenshot({ path: path.join(output, 'dropdown-mobile.png') });
        await page.tap(trigger);
        assert.equal(await expanded(), 'false', 'Touch toggles closed');
        await page.tap(trigger);
        await Promise.all([page.waitForNavigation({ waitUntil: 'networkidle2' }), page.tap('.support-nav-menu li:first-child a')]);
        assert.equal(new URL(page.url()).pathname, routes[0]);
        report.push({ dropdown: 'mobile', touch: 'OK', navigate: 'OK' });

        await page.tap('.support-faq summary');
        assert.ok(await page.$eval('.support-faq details', e => e.open), 'FAQ opens');
        await page.click('.support-toc a[href="#theo-doi"]');
        assert.equal(new URL(page.url()).hash, '#theo-doi');
        report.push({ faq: 'OK', anchors: 'OK', pageErrors: errors });
        assert.deepEqual(errors, [], 'Browser runtime errors');
        fs.writeFileSync(path.join(output, 'browser-report.json'), JSON.stringify(report, null, 2));
        console.log(JSON.stringify(report, null, 2));
    } finally {
        await browser.close();
        await new Promise(resolve => server.close(resolve));
    }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
