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

-- Populate categories with default data
INSERT INTO categories (name, color) VALUES 
('Alimentação', '#FF6B6B'),
('Transporte', '#4ECDC4'),
('Saúde', '#45B7D1'),
('Educação', '#96CEB4'),
('Lazer', '#FFEAA7'),
('Vestuário', '#DDA0DD'),
('Moradia', '#98D8C8'),
('Tecnologia', '#F7DC6F'),
('Serviços', '#BB8FCE'),
('Viagem', '#85C1E9'),
('Supermercado', '#F8C471'),
('Restaurante', '#E74C3C'),
('Combustível', '#2ECC71'),
('Farmácia', '#E67E22'),
('Shopping', '#9B59B6'),
('Outros', '#95A5A6')
ON DUPLICATE KEY UPDATE name = VALUES(name), color = VALUES(color); 