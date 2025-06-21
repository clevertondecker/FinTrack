-- Populate banks with test data
INSERT INTO banks (code, name) VALUES 
('NU', 'Nubank'),
('ITAU', 'Itaú Unibanco'),
('SAN', 'Santander'),
('BB', 'Banco do Brasil'),
('BRADESCO', 'Bradesco'),
('CAIXA', 'Caixa Econômica Federal'),
('INTER', 'Banco Inter'),
('C6', 'C6 Bank'),
('PICPAY', 'PicPay'),
('MERCADOPAGO', 'Mercado Pago')
ON DUPLICATE KEY UPDATE name = VALUES(name); 