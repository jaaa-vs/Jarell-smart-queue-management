-- Smart Queue Management System DB Setup
-- Run in phpMyAdmin after creating 'queue_system' DB

CREATE DATABASE IF NOT EXISTS `queue_system`
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `queue_system`;

CREATE TABLE IF NOT EXISTS `queues` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `queue_num` VARCHAR(10) NOT NULL UNIQUE,
  `gen_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `called_time` TIMESTAMP NULL,
  `status` ENUM('waiting', 'calling', 'served') DEFAULT 'waiting',
  INDEX `idx_status_time` (`status`, `gen_time`)
) ENGINE=InnoDB;

-- Test data
INSERT IGNORE INTO `queues` (`queue_num`, `status`, `gen_time`) VALUES
('A001', 'served', '2024-10-01 08:30:00'),
('A002', 'served', '2024-10-01 08:32:00'),
('A013', 'waiting', '2024-10-01 09:25:00');

-- Clear queue fn (call when reset)
DELIMITER //
CREATE PROCEDURE `clear_waiting_queues`()
BEGIN
  DELETE FROM `queues` WHERE `status` IN ('waiting', 'next_batch');
END//
DELIMITER ;

-- Add new status (run once)
ALTER TABLE `queues` MODIFY COLUMN `status` ENUM('waiting', 'next_batch', 'calling', 'served') DEFAULT 'waiting';

