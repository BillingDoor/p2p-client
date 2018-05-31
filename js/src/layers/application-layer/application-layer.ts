import { Address } from '@models';
import { BusinessLayer } from '@layers/business-layer/business-layer';

export class ApplicationLayer {
  constructor(private worker: BusinessLayer) {}

  launchClient(bootstrapNode: Address) {
    this.worker.joinNetwork(bootstrapNode);
  }
}
