import { OTHER } from '@/constants';

import AbstractComp from './AbstractComp';

export default class PopComp extends AbstractComp {

  static getSupportedTypes() {
    return [
      OTHER.POP_ADD, OTHER.POP_EDIT,
      OTHER.POP_BATCH, OTHER.POP_FLOW
    ];
  }

  static getSupportedDomain() {
    return true;
  }

  pageCloneItem(that, domainName, linkItem, componentItem) {
    return super.pageCloneItem(that, domainName, linkItem, componentItem)
  }

}