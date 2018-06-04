import { Address } from '@models';
import { BusinessLayer } from '@layers/business-layer/business-layer';
import logger from '@utils/logging';

export class ApplicationLayer {
  constructor(private worker: BusinessLayer) {}

  async launch(bootstrapNode: Address) {
    try {
      await this.worker.joinNetwork(bootstrapNode);
    } catch (e) {
      logger.error("Application layer: couldn't connect to bootstrap node.");
    }
  }

  close() {
    this.worker.close();
  }
}
