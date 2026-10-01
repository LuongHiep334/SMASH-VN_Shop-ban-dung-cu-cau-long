// Run with: node --test src/test/js/customer-search.test.cjs
// Requires Puppeteer (normal module lookup, PUPPETEER_MODULE_PATH, or the local scratch install).
const {test,before,after,beforeEach,afterEach}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const root=path.resolve(__dirname,'../../..');
let puppeteer;
try { puppeteer=require(process.env.PUPPETEER_MODULE_PATH||'puppeteer'); }
catch { puppeteer=require(path.join(root,'scratch/node_modules/puppeteer')); }
const header=fs.readFileSync(path.join(root,'src/main/resources/templates/layout/header.html'),'utf8');
// Keep the real header layout: testing a form forced to width:100% missed the narrow mobile dropdown.
const navigation=header.match(/<nav class="primary-nav primary-nav-wrapper--border">[\s\S]*?<\/nav>/)[0]
 .replace('th:action="@{/shop}"','action="/shop"').replace('th:src="@{/images/logo/logo-2.png}"','src="/images/logo/logo-2.png"');
const css=['vendor','utility','app'].map(n=>fs.readFileSync(path.join(root,'src/main/resources/static/css',n+'.css'),'utf8')).join('\n');
const html='<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>'+css+'</style></head><body><header class="header--style-1">'+navigation+'</header><h1 style="font-size:18px;text-align:center">Header đầy đủ — dữ liệu kiểm thử</h1><button id="outside" style="position:fixed;bottom:8px;left:8px">Ngoài ô tìm kiếm</button><script src="/js/customer-search.js"></script></body></html>';
const item=(name,id=27)=>({id,tenSanPham:name,thuongHieu:'Yonex',danhMuc:'Vợt cầu lông',giaBan:1000000,giaSauGiam:750000,giamGia:25,conHang:true,nhieuMucGia:true});
let server,browser,page,origin,control,requests;
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
before(async()=>{
 server=http.createServer((req,res)=>{
  const url=new URL(req.url,'http://localhost');
  if(url.pathname==='/js/customer-search.js'){res.setHeader('Content-Type','application/javascript');return res.end(fs.readFileSync(path.join(root,'src/main/resources/static/js/customer-search.js')));}
  if(url.pathname==='/images/logo/logo-2.png'){res.setHeader('Content-Type','image/png');return res.end(fs.readFileSync(path.join(root,'src/main/resources/static/images/logo/logo-2.png')));}
  if(/^\/(fonts|webfonts)\/[a-zA-Z0-9_./-]+\.(woff2?|ttf|eot)$/.test(url.pathname)&&!url.pathname.includes('..')){
   const font=path.join(root,'src/main/resources/static',url.pathname);
   if(fs.existsSync(font)){res.setHeader('Content-Type',url.pathname.endsWith('.woff2')?'font/woff2':'application/octet-stream');return res.end(fs.readFileSync(font));}
  }
  if(url.pathname.startsWith('/api/search/')){
   const q=url.searchParams.get('q')||'';requests.push(q);
   const popular=url.pathname.endsWith('/popular');
   let data=popular?{danhMuc:['Vợt cầu lông','Giày cầu lông','Áo cầu lông','Quần cầu lông','Balo cầu lông','Túi cầu lông','Dây cước','Quấn cán'].map((ten,i)=>({id:i+1,ten})),thuongHieu:['Yonex','Li-Ning','Victor','Mizuno','GOSEN','Kizuna'].map((ten,i)=>({id:i+1,ten})),sanPhamNoiBat:[item('Vợt nổi bật'),item('Vợt cầu lông Yonex Nanoflare 700 Pro 2024 phiên bản đặc biệt',28),item('Giày cầu lông Lining AYZW007-3 chính hãng',29),item('Quấn cán Yonex xin AC102-30 EX (Túi 2 cuộn)',30)]}:
       q==='none'?[]:[item('Vợt '+q),{...item('Vợt hết hàng',28),giaBan:0,giaSauGiam:0,conHang:false,nhieuMucGia:false}];
   const fail=control.failOnce&&!popular;if(fail)control.failOnce=false;
   const delay=popular?control.popularDelay:(control.delays[q]||0);
   return setTimeout(()=>{if(!res.destroyed){res.writeHead(fail?503:200,{'Content-Type':'application/json'});res.end(JSON.stringify(data));}},delay);
  }
  res.setHeader('Content-Type','text/html; charset=utf-8');res.end(html);
 });
 await new Promise(r=>server.listen(0,'127.0.0.1',r));origin='http://127.0.0.1:'+server.address().port;
 browser=await puppeteer.launch({headless:true,args:['--no-sandbox']});
});
after(async()=>{if(browser)await browser.close();if(server)await new Promise(r=>server.close(r));});
beforeEach(async()=>{control={failOnce:false,delays:{},popularDelay:0};requests=[];page=await browser.newPage();await page.setViewport({width:390,height:844});await page.goto(origin,{waitUntil:'networkidle0'});});
afterEach(async()=>{await page.close();});
async function input(q){await page.focus('#main-search');await page.$eval('#main-search',(e,q)=>{e.value=q;e.dispatchEvent(new Event('input',{bubbles:true}));},q);}
async function result(q){await page.waitForFunction(q=>document.querySelector('.autocomplete-name')?.textContent==='Vợt '+q,{},q);}
async function shot(name){if(process.env.SEARCH_TEST_OUTPUT){fs.mkdirSync(process.env.SEARCH_TEST_OUTPUT,{recursive:true});await page.screenshot({path:path.join(process.env.SEARCH_TEST_OUTPUT,name+'.png')});}}

test('S01 text-only suggestions, price from and sold-out/zero-price states',async()=>{
 await input('Yonex');await result('Yonex');
 assert.equal(await page.$$eval('#search-autocomplete img',x=>x.length),0);
 assert.equal(await page.$eval('#main-search',e=>e.getAttribute('aria-expanded')),'true');
 const text=await page.$eval('#search-autocomplete',e=>e.textContent);
 assert.match(text,/Từ 750\.000/);assert.match(text,/0 ₫/);assert.match(text,/Tạm hết hàng/);
 await shot('search-text-only-390');
});
test('S01 popular suggestions also have no product images',async()=>{
 await page.focus('#main-search');await page.waitForSelector('.popular-tag');
 assert.equal(await page.$$eval('#search-autocomplete img',x=>x.length),0);
 assert.equal(await page.$$eval('.autocomplete-item',x=>x.length),4);
});
test('S06 out-of-order response cannot overwrite newest query even when abort is ignored',async()=>{
 await page.evaluate(()=>{const original=fetch;window.fetch=(url,options)=>original(url,{...options,signal:undefined});});
 control.delays.old=1000;await input('old');await page.waitForFunction(()=>document.querySelector('#main-search').getAttribute('aria-busy')==='true');
 await input('new');await result('new');await sleep(1100);assert.equal(await page.$eval('.autocomplete-name',e=>e.textContent),'Vợt new');
});
test('S06 delayed popular response cannot replace typed results',async()=>{
 control.popularDelay=900;await page.evaluate(()=>{const original=fetch;window.fetch=(url,options)=>original(url,{...options,signal:undefined});});
 await page.focus('#main-search');await sleep(60);await input('Yonex');await result('Yonex');await sleep(1000);
 assert.equal(await page.$$eval('.popular-tag',x=>x.length),0);
});
test('S06 Escape invalidates pending response',async()=>{
 control.delays.late=600;await input('late');await page.waitForFunction(()=>document.querySelector('#main-search').getAttribute('aria-busy')==='true');
 await page.keyboard.press('Escape');await sleep(750);
 assert.equal(await page.$eval('#main-search',e=>e.getAttribute('aria-expanded')),'false');
});
test('S06 deleting input shows popular instead of late product results',async()=>{
 control.delays.late=700;await input('late');await sleep(400);await input('');await page.waitForSelector('.popular-tag');await sleep(750);
 assert.equal(await page.$eval('.autocomplete-name',e=>e.textContent),'Vợt nổi bật');
});
test('S08 keyboard chooses suggestion and Enter navigates to it',async()=>{
 await input('Yonex');await result('Yonex');await page.keyboard.press('ArrowDown');
 assert.equal(await page.$eval('#main-search',e=>e.getAttribute('aria-activedescendant')),'search-option-0');
 const navigation=page.waitForNavigation();await page.keyboard.press('Enter');await navigation;
 assert.equal(new URL(page.url()).pathname,'/san-pham/27');
});
test('S08 ArrowUp wraps and Escape resets active option',async()=>{
 await input('Yonex');await result('Yonex');await page.keyboard.press('ArrowUp');
 assert.equal(await page.$eval('#main-search',e=>e.getAttribute('aria-activedescendant')),'search-option-2');
 await page.keyboard.press('Escape');assert.equal(await page.$eval('#main-search',e=>e.getAttribute('aria-activedescendant')),null);
});
test('S08 Enter without active option preserves normal search form submission',async()=>{
 await input('Yonex');await result('Yonex');const navigation=page.waitForNavigation();await page.keyboard.press('Enter');await navigation;
 assert.equal(new URL(page.url()).searchParams.get('q'),'Yonex');assert.equal(new URL(page.url()).pathname,'/shop');
});
test('S08 loading/error/retry states recover',async()=>{
 control.failOnce=true;control.delays.Yonex=250;await input('Yonex');
 await page.waitForFunction(()=>document.querySelector('#search-status').textContent==='Đang tìm sản phẩm.');
 await page.waitForSelector('[data-search-retry]');await shot('search-error-retry');await page.click('[data-search-retry]');await result('Yonex');
 assert.equal(await page.$eval('#main-search',e=>e.getAttribute('aria-busy')),'false');
});
test('S08 empty response has distinct status and closes on Escape',async()=>{
 await input('none');await page.waitForFunction(()=>document.querySelector('#search-status').textContent==='Không tìm thấy sản phẩm.');
 assert.equal(await page.$$eval('[data-search-retry]',x=>x.length),0);await page.keyboard.press('Escape');
 assert.equal(await page.$eval('#search-autocomplete',e=>getComputedStyle(e).display),'none');
});
test('S08 clicking or focusing outside closes suggestions',async()=>{
 await input('Yonex');await result('Yonex');await page.click('#outside');
 assert.equal(await page.$eval('#main-search',e=>e.getAttribute('aria-expanded')),'false');
});
test('S08 IME composition does not query partially composed text',async()=>{
 await page.focus('#main-search');await sleep(70);requests=[];
 await page.$eval('#main-search',e=>{e.dispatchEvent(new CompositionEvent('compositionstart'));e.value='Vợ';e.dispatchEvent(new Event('input',{bubbles:true}));});
 await sleep(400);assert.equal(requests.includes('Vợ'),false);
 await page.$eval('#main-search',e=>{e.value='Vợt';e.dispatchEvent(new CompositionEvent('compositionend'));});await result('Vợt');
});
test('Responsive search fills its header row and dropdown stays inside viewport',async()=>{
 for(const [w,h] of [[320,568],[375,667],[390,844],[667,375],[768,650],[1024,768],[1440,900]]){
  await page.setViewport({width:w,height:h});await input('');await page.waitForSelector('.popular-tag');
  const metrics=await page.evaluate(()=>{const box=s=>{const r=document.querySelector(s).getBoundingClientRect();return{left:r.left,right:r.right,top:r.top,bottom:r.bottom,width:r.width}};return{input:box('#main-search'),popup:box('#search-autocomplete'),logo:box('.main-logo'),menu:box('#navigation'),row:box('.primary-nav .primary-nav'),scroll:document.documentElement.scrollWidth};});
  assert(metrics.popup.left>=0&&metrics.popup.right<=w+1);assert(metrics.popup.bottom<=h-11);assert(metrics.scroll<=w+1);
  if(w<=767){assert(metrics.input.width>=metrics.row.width-1);assert(metrics.input.top>=metrics.logo.bottom);assert(metrics.input.top>=metrics.menu.bottom);}
  else {assert(metrics.input.width>=320);assert(metrics.input.left>=metrics.logo.right);assert(metrics.input.right<=metrics.menu.left);}
  assert.equal(await page.$eval('.popular-tag',e=>e.getBoundingClientRect().height<44),true,'ordinary category tag should fit on one line');
  await shot('header-popular-'+w);
  await input('Yonex');await result('Yonex');await shot('search-text-only-'+w);
 }
});

test('Responsive dropdown follows a reduced visual viewport and offset while keyboard is open',async()=>{
 await page.evaluate(()=>{
  const viewport=new EventTarget();viewport.height=window.innerHeight;viewport.offsetTop=0;
  Object.defineProperty(window,'visualViewport',{value:viewport,configurable:true});
 });
 // Reload the real script in a fresh page so listeners attach to the emulated visual viewport.
 await page.evaluateOnNewDocument(()=>{const viewport=new EventTarget();viewport.height=window.innerHeight;viewport.offsetTop=0;Object.defineProperty(window,'visualViewport',{value:viewport,configurable:true});});
 await page.reload({waitUntil:'networkidle0'});await page.focus('#main-search');await page.waitForSelector('.popular-tag');
 for(const [height,offsetTop] of [[330,0],[280,40]]){
  await page.evaluate(([height,offsetTop])=>{visualViewport.height=height;visualViewport.offsetTop=offsetTop;visualViewport.dispatchEvent(new Event('resize'));},[height,offsetTop]);
  await page.waitForFunction(()=>document.querySelector('#search-autocomplete').getBoundingClientRect().bottom<=visualViewport.height+visualViewport.offsetTop-11);
  assert.equal(await page.$eval('#main-search',e=>e.getAttribute('aria-expanded')),'true');
 }
 await shot('header-keyboard-viewport');
});

test('Responsive popular list can scroll to last product without moving the page',async()=>{
 await page.setViewport({width:375,height:330});await page.focus('#main-search');await page.waitForSelector('.popular-tag');
 await page.$eval('#search-autocomplete',e=>{e.scrollTop=e.scrollHeight;});
 const metrics=await page.$eval('#search-autocomplete',e=>{const last=e.querySelector('.autocomplete-item:last-child').getBoundingClientRect(),r=e.getBoundingClientRect();return{scroll:e.scrollTop,lastBottom:last.bottom,popupBottom:r.bottom,pageScroll:scrollY};});
 assert(metrics.scroll>0);assert(metrics.lastBottom<=metrics.popupBottom+1);assert.equal(metrics.pageScroll,0);
 await page.keyboard.press('ArrowUp');await page.waitForFunction(()=>document.querySelector('[aria-selected="true"]')?.textContent.includes('Túi 2 cuộn'));
});
