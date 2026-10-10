import activityConfig from '@/components/page_pop/configs/appActivityPopConfig'
import GActivityPop from '@/components/page_pop/GActivityPop.vue'
import { postAction } from '@/api/api'
import GPlainPop from '@/components/page_pop/GPlainPop.vue'
import {vTableUtil} from "@/utilsv/vUtil";


let utilObj = {
  showScheduleModal: function(that, record, context, successCallback, cancelCallback){
    let dynComp = undefined
    // 创建动态组件
    dynComp = that.$dynamicModal({
      configObj: activityConfig.popConfig,
    }, GActivityPop)
    // 展示弹窗组件
    dynComp.showPop(record, context, (record, context) => {
      context || true
      /* submit */
      let updateUri = `/gcsActivity/updateSchedule`
      let saveUri = `/gcsActivity/saveSchedule?domainName=${context.domainName}&domainId=${context.recordId}`
      let promise = postAction(record.id?updateUri:saveUri, record)
      promise.then(resp => {
        let respRecord = resp.gmodel
        successCallback(respRecord, context)
        that.$message.success(`${record.id?'更新':'保存'}日程成功`);
        if(that._.get(record, '_opType') === 2){
          utilObj.showScheduleModal(that, {}, {})
        }
      }).catch(err => {
        vTableUtil.tableReqErr(that, err, `${record.id?'更新':'保存'}日程失败`)
      })
      return promise
    }, cancelCallback)
  },
  showFinishModal: function(that, record, context, successCallback, cancelCallback){
    let dynComp = undefined
    let comment = ''
    let loadingObj = { }
    // 定义弹窗内容
    let createContent = () => {
      return function(h, slotProps) {
        h || slotProps || true
        let vnodes = [];
        vnodes.push(
          <ATextarea {...{
            attrs: { rows: 4, },
            props: {
              value: comment,
              placeholder: '备注...'
            },
            on: {
              change: (e) => {
                comment = e.target.value
              },
            }
          }}>
          </ATextarea>
        )
        return (<div {...{ class: 'finish-wrapper' }}>{vnodes}</div>)
      }
    }
    // 定义底部按钮
    let createFooter = () => {
      return function(h, slotProps) {
        h || slotProps || true
        let vnodes = [];
        vnodes.push(<div></div>)
        let doneActivity = (_opType, _opIdent) => {
          if (dynComp) {
            let loadingStr = _opIdent
            let submitObj = {
              id: record['id'],
              comment: comment,
              _opType: _opType,
            }
            let promise = dynComp.onSubmit(submitObj)
            if (promise && promise.then) {
              loadingObj[loadingStr] = true
              dynComp.update({
                footer: createFooter(),
              })
              promise.then(() => {
                loadingObj[loadingStr] = false
                dynComp.update({
                  footer: createFooter(),
                })
                if(submitObj['_opType'] === 2){
                  utilObj.showScheduleModal(that, {}, {})
                }
              }).catch(() => {
                loadingObj[loadingStr] = false
                dynComp.update({
                  footer: createFooter(),
                })
              })
            }
          }
        }
        return (
          <div class="flex flex-row main-between cross-center">
            <div>
              {vnodes}
            </div>
            <div>
              {(
                <div>
                  <a-button icon={'check-circle'} loading={loadingObj['doneAndCreate']}
                            onClick={() => doneActivity(2, 'doneAndCreate')}>完成并新建
                  </a-button>
                  <a-button type={'primary'} icon={'check'} loading={loadingObj['doneOnly']}
                            onClick={() => doneActivity(1, 'doneOnly')}>完成
                  </a-button>
                </div>
              )}
            </div>
          </div>
        )
      }
    }
    // 创建动态组件
    dynComp = that.$dynamicModal({
      configObj: {
        modal: true,
        title: `完成日程 | ${record['title']}`,
      },
      content: createContent(),
      footer: createFooter()
    }, GPlainPop)
    // 展示弹窗组件
    dynComp.showPop(record, {},
      (record, context) => { /* submit */
        context || true
        let promise = postAction(`/gcsActivity/finishSchedule`, {
          id: record['id'],
          comment: record['comment'],
        })
        promise.then(resp => {
          let respRecord = resp.gmodel
          successCallback(respRecord, context)
          that.$message.success(`完成日程成功`);
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, '完成失败')
        })
        return promise
      }, cancelCallback)
  },
}

export default utilObj