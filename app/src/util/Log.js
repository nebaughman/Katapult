import * as logger from 'loglevel'

export const Log = {

  debug(...msgs) {
    logger.debug(this.format(msgs))
  },

  info(...msgs) {
    logger.info(this.format(msgs))
  },

  warn(...msgs) {
    logger.warn(this.format(msgs))
  },

  fail(...msgs) {
    logger.error(this.format(msgs))
  },

  error(...msgs) {
    logger.error(this.format(msgs))
  },

  format(...msgs) {
    return msgs ? msgs.join(', ') : "" // TODO: better formatting
  },

}
