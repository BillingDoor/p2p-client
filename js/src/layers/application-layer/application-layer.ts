import { Address } from '@models';
import { BusinessLayer } from '@layers/business-layer/business-layer';

export class ApplicationLayer {
  constructor(private worker: BusinessLayer) {}

  launch(bootstrapNode: Address) {
    this.worker.joinNetwork(bootstrapNode);
  }

  close() {
    this.worker.close();
  }
}
