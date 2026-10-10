import moment from "moment"
import DATE from '@/constants/consts-date'

const DAYS = 10
const DAY_MILLS = 24*60*60*1000

function mockDataDimension2Func(){
  let mockData = []
  let beginDay = new Date().getTime()
  for (let i = 0; i < DAYS; i++) {
    let date = new Date(beginDay - DAY_MILLS * (DAYS - i - 1))
    mockData.push({
      label: moment(date).format(DATE.YYYYMMDD),
      value: Math.round(Math.random() * 10)
    })
  }
  return mockData
}

function mockDataDimension3Func(){
  let mockData = []
  let beginDay = new Date().getTime()
  for (let i = 0; i < DAYS; i++) {
    let date = new Date(beginDay - DAY_MILLS * (DAYS - i - 1))
    mockData.push({
      type: 'type1',
      label: moment(date).format(DATE.YYYYMMDD),
      value: Math.round(Math.random() * 10)
    })
  }
  for (let i = 0; i < DAYS; i++) {
    let date = new Date(beginDay - DAY_MILLS * (DAYS - i - 1))
    mockData.push({
      type: 'type2',
      label: moment(date).format(DATE.YYYYMMDD),
      value: Math.round(Math.random() * 10)
    })
  }
  return mockData
}

export default {
  code: '0',
  message: null,
  gmodel: {
    mockData2Func: mockDataDimension2Func,
    mockData3Func: mockDataDimension3Func,
  }
}
