import * as logger from 'loglevel'

export class Logger {

  public debug(...msgs: any) {
    logger.debug(this.format(msgs))
  }

  public info(...msgs: any) {
    logger.info(this.format(msgs))
  }

  public warn(...msgs: any): void {
    logger.warn(this.format(msgs))
  }

  public fail(...msgs: any) {
    logger.error(this.format(msgs))
  }

  private format(...msgs: any) {
    return msgs.join(', ') // TODO: better formatting
  }

}

/**
 * Singleton instance
 */
export const Log = new Logger()