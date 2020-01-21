import * as logger from 'loglevel'

export class Log {

  static debug(...msgs: any[]) {
    logger.debug(this.format(msgs))
  }

  static info(...msgs: any[]) {
    logger.info(this.format(msgs))
  }

  static warn(...msgs: any[]) {
    logger.warn(this.format(msgs))
  }

  static fail(...msgs: any[]) {
    logger.error(this.format(msgs))
  }

  static error(...msgs: any[]) {
    logger.error(this.format(msgs))
  }

  private static format(...msgs: any[]) {
    return msgs ? msgs.join(', ') : "" // TODO: better formatting
  }

}
