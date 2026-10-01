import React from 'react';
import { SePayPgClient } from 'sepay-pg-node';

export default function SePaySandbox() {
  const client = new SePayPgClient({
    env: 'sandbox',
    merchant_id: 'SP-TEST-LT76BA77',
    secret_key: 'spsk_test_yiKXykZiWbFt9jFUJFEoPJfERNPLwsbL'
  });

  const checkoutURL = client.checkout.initCheckoutUrl();

  const checkoutFormfields = client.checkout.initOneTimePaymentFields({
    payment_method: 'BANK_TRANSFER',
    order_invoice_number: 'DH123',
    order_amount: 10000,
    currency: 'VND',
    order_description: 'Thanh toan don hang DH123',
    success_url: 'https://example.com/order/DH123?payment=success',
    error_url: 'https://example.com/order/DH123?payment=error',
    cancel_url: 'https://example.com/order/DH123?payment=cancel',
  });

  return (
    <form action={checkoutURL} method="POST">
      {Object.keys(checkoutFormfields).map(field => (
        <input key={field} type="hidden" name={field} value={checkoutFormfields[field]} />
      ))}
      <button type="submit">Pay now</button>
    </form>
  );
}
