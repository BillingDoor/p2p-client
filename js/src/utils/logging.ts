import { createLogger, format, transports } from 'winston';

const loggingFormat = format.printf((info) => {
  return `[${info.level}]: ${info.message}`;
});

const logger = createLogger({
  format: format.combine(format.colorize(), loggingFormat),
  transports: [
    new transports.Console(),
    new transports.File({ filename: 'dist/app.log' })
  ]
});

export default logger;
