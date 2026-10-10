import moment from 'moment';
import _ from '@/utils/tiny_lodash'


let utilObj = {
  typeIcon(sciScheduleType){
    let typeToIcon = {
      101: 'g-todo',           // 待办
      102: 'g-meeting',        // 会议
      103: 'g-email',          // 发邮件
      104: 'g-call',           // 打电话
      105: 'g-todo',          // 仅提醒
    }
    return _.get(typeToIcon, sciScheduleType, 'g-email')
  },
  dateStrAndClass(dataStr){
    let dueMoment = moment(new Date(dataStr))
    let todayMidnight = moment().startOf('day');
    let yesterdayMidnight = todayMidnight.clone().add(-1, 'days');
    let tomorrowMidnight = todayMidnight.clone().add(1, 'days');
    const isToday = dueMoment.isSame(todayMidnight, 'day');
    const isYesterday = dueMoment.isSame(yesterdayMidnight, 'day');
    const isTomorrow = dueMoment.isSame(tomorrowMidnight, 'day') && false;
    let dueMomentStr = undefined;
    if(isToday || isYesterday || isTomorrow) {
      dueMomentStr = dueMoment.calendar(null, {
        sameDay: '[今天]',
        lastDay: '[昨天]',
        nextDay: '[明天截止]',
      });
    }else{
      dueMomentStr = dueMoment.from(todayMidnight) + '截止'
    }
    // 今天零点之前error，零点或零点之后success
    return [dueMomentStr, dueMoment.isBefore(yesterdayMidnight)?'error-dark':dueMoment.isBefore(todayMidnight)?'warning-dark':'success-dark']
  }
}
export default utilObj
