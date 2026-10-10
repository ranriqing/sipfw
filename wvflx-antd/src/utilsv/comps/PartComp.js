import { OTHER } from '@/constants'

import AbstractComp from './AbstractComp';


export default class PartComp extends AbstractComp {

  static getSupportedTypes() {
    return [
      OTHER.PART_QUERY, OTHER.PART_TABLE, OTHER.PART_CHART,
      OTHER.PART_REPORT, OTHER.PART_DYNRPT, OTHER.PART_MDOWN,
      OTHER.PART_WIDGET,
    ];
  }

  static getSupportedDomain() {
    return true;
  }

  pageCloneItem(that, domainName, linkItem, componentItem) {
    return super.pageCloneItem(that, domainName, linkItem, componentItem)
  }
}