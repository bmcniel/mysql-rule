create table transaction(
  id int not null auto_increment primary key,
  uuid varchar(36) not null comment 'The UUID business key for this transaction',
  site_uuid varchar(36) not null comment 'The UUID of the site where the transaction took place',
  user_uuid varchar(36) null default null comment 'The UUID of the Upside user conducting the transaction',
  timestamp bigint not null comment 'The epoch millis when the transaction took place',
  card_type varchar(12) not null comment 'The type of the credit card the transaction was for',
  cc_first_six varchar(6) not null comment 'The first six digits of the credit card',
  cc_last_four varchar(4) not null comment 'The last four digits of the credit card',
  amount decimal(10, 2) not null comment 'The total amount of the transaction',
  currency_code varchar(4) not null comment 'The currency of the transaction',
  source_terminal varchar(32) not null comment 'The id of the site''s terminal where the transaction took place',
  status varchar (32) not null comment 'The current status of the transaction',
  unique key transaction_uk (site_uuid, timestamp, card_type, cc_first_six, cc_last_four, amount, currency_code, source_terminal)
);