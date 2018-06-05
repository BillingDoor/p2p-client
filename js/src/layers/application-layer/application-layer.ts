import { Address } from '@models';
import { BusinessLayer } from '@layers/business-layer/business-layer';
import logger from '@utils/logging';

export class ApplicationLayer {
  constructor(private worker: BusinessLayer) {}

  async launch(bootstrapNode: Address) {
    try {
      await this.worker.joinNetwork(bootstrapNode);
    } catch (err) {
      logger.error("Application layer: couldn't connect to bootstrap node.");
    }
  }

  close() {
    logger.info('Application layer: closing.');
    this.worker.close();
  }
}
