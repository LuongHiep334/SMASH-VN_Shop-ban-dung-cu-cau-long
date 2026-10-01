const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
require('../../main/resources/static/js/checkout-request.js');

const flush = () => new Promise(resolve => setImmediate(resolve));

function fixture(post, getStatus) {
    const events = { busy: [], results: [], notices: [], timers: [], posts: 0, reads: 0 };
    const flow = globalThis.createCheckoutRequest({
        post: data => { events.posts++; return post(data); },
        getStatus: () => { events.reads++; return getStatus(); },
        onResult: response => events.results.push(response),
        onBusy: busy => events.busy.push(busy),
        onUncertain: (message, retry) => events.notices.push({ message, retry }),
        schedule: callback => events.timers.push(callback)
    });
    return { flow, events };
}

test('validation failure allows a corrected submission; confirmation blocks double clicks', async () => {
    const { flow, events } = fixture(() => ({ trangThai: 'loi', message: 'Invalid phone' }));
    assert.equal(flow.begin(), true);
    assert.equal(flow.begin(), false);
    flow.send({ checkoutToken: 'original' });
    await flush();
    assert.equal(events.busy.at(-1), false);
    assert.equal(flow.begin(), true);
    assert.equal(events.posts, 1);
});

test('cancelling confirmation releases the button without submitting', () => {
    const { flow, events } = fixture(() => assert.fail('Must not submit'));
    flow.begin();
    flow.cancel();
    assert.equal(flow.begin(), true);
    assert.equal(events.posts, 0);
});

test('lost response recovers the committed order through GET without a second POST', async () => {
    const result = { trangThai: 'ok', checkoutStatus: 'COMPLETED', orderId: 501, paymentMethod: 'COD' };
    const { flow, events } = fixture(() => Promise.reject(new Error('Connection lost')), () => result);
    flow.begin();
    flow.send({ checkoutToken: 'original' });
    await flush();
    assert.deepEqual(events.results, [result]);
    assert.equal(events.posts, 1);
    assert.equal(events.reads, 1);
    assert.equal(flow.begin(), false);
});

test('processing response keeps submit locked while polling then opens saved result', async () => {
    const { flow, events } = fixture(
        () => ({ trangThai: 'processing', checkoutStatus: 'PROCESSING' }),
        () => ({ trangThai: 'ok', checkoutStatus: 'COMPLETED', orderId: 501 })
    );
    flow.begin();
    flow.send({});
    await flush();
    assert.equal(flow.begin(), false);
    assert.equal(events.timers.length, 1);
    events.timers.shift()();
    await flush();
    assert.equal(events.results[0].orderId, 501);
    assert.equal(events.posts, 1);
});

test('recovery finding READY permits manual correction and retry', async () => {
    const { flow, events } = fixture(() => Promise.reject(), () => ({ trangThai: 'ready', checkoutStatus: 'READY' }));
    flow.begin();
    flow.send({});
    await flush();
    assert.equal(flow.begin(), true);
    assert.equal(events.posts, 1);
});

test('unavailable status offers a read-only retry and does not unlock uncertain submission', async () => {
    let available = false;
    const { flow, events } = fixture(() => Promise.reject(), () => {
        if (!available) return Promise.reject();
        return { trangThai: 'ok', checkoutStatus: 'COMPLETED', orderId: 501 };
    });
    flow.begin();
    flow.send({});
    await flush();
    assert.equal(events.notices.at(-1).retry, true);
    assert.equal(flow.begin(), false);
    available = true;
    flow.recover();
    flow.recover();
    await flush();
    assert.equal(events.posts, 1);
    assert.equal(events.reads, 2);
    assert.equal(events.results[0].orderId, 501);
});

test('polling is bounded and can be resumed without creating another order', async () => {
    const pending = () => ({ trangThai: 'processing', checkoutStatus: 'PROCESSING' });
    const { flow, events } = fixture(pending, pending);
    flow.begin();
    flow.send({});
    await flush();
    for (let i = 0; i < 15; i++) {
        assert.equal(events.timers.length, 1);
        events.timers.shift()();
        await flush();
    }
    assert.equal(events.timers.length, 0);
    assert.equal(events.reads, 15);
    assert.equal(events.notices.at(-1).retry, true);
    assert.equal(flow.begin(), false);
    assert.equal(events.posts, 1);
});

test('checkout template inline scripts remain syntactically valid after wiring recovery', () => {
    const html = fs.readFileSync('src/main/resources/templates/checkout.html', 'utf8');
    for (const [, script] of html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/g)) {
        if (script.trim()) new vm.Script(script);
    }
});
