import Vue from "vue";
import * as dayjs from "dayjs";
import moment from '@/utils/moment'
import DATE from '@/constants/consts-date'

/**
 * Eg: format 6777888 to 6,777,888
 */
Vue.filter('NumberFormat', function (value) {
  if (!value) { return '0' }
  return value.toString().replace(/(\d)(?=(?:\d{3})+$)/g, '$1,')
})


/**
 * Eg: format '2018/01/01 00:00:00' to '1 years ago'
 */
Vue.filter('PrettyDate', function(dataStr) {
  return dataStr?moment(new Date(dataStr)).fromNow():'暂无'
})

/**
 * Eg: format '59001' to '5w'
 */
Vue.filter('PrettyInteger', function (value, unit) {
  value = parseInt(value)
  if(!value && value!=0){
    return ''
  }
  try {
    unit = unit || '';
    let v = value;
    if (v >= 100000000) {
      v = (v / 100000000).toFixed(2) + '亿'+unit
      return v
    } else if (v >= 10000000) {
      v = (v / 10000000).toFixed(2) + '千万'+unit
      return v
    } else if (v >= 10000000) {
      v = (v / 10000000).toFixed(2) + '百万'+unit
      return v
    } else if (v >= 10000) {
      v = (v / 10000).toFixed(1) + '万'+unit
      return v
    } else {
      return v.toFixed(0) +''+unit
    }
  } catch (e) {
    console.log(e)
    return 0;
  }
})

Vue.filter('SipPrettyInteger', function (value, unit) {
  value = parseInt(value)
  if(!value && value!=0){
    return ''
  }
  try {
    unit = unit || '';
    let v = value;
    if (v >= 10000000) {
      v = (v / 10000000).toFixed(2) + 'm'+unit
      return v
    } else if (v >= 10000) {
      v = (v / 10000).toFixed(1) + 'w'+unit
      return v
    } else if (v >= 1000) {
      v = (v / 1000).toFixed(1) + 'k'+unit
      return v
    } else {
      return v.toFixed(0) +''+unit
    }
  } catch (e) {
    console.log(e)
    return 0;
  }
})

/**
 * Eg: format '20180101' to '2018/01/01 00:00:00'
 */
Vue.filter('dayjs', function(dataStr, pattern = DATE.YYYYMMDD_HHMMSS) {
  return dayjs(dataStr).format(pattern)
})

/**
 * Cut long string to short
 */
Vue.filter('Ellipsis', function (value, vlength = 25) {
  if(!value){ return '' }
  if (value.length > vlength) {
    return value.slice(0, vlength) + '...'
  }
  return value
})