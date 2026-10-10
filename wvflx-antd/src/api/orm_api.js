// Http请求
import {
  getAction as getActionOrm,
  deleteAction as deleteActionOrm,
  putAction as putActionOrm,
  patchAction as patchActionOrm,
  postAction as postActionOrm,
} from '@/api/orm_http_request'

export default {
  interceptors:{
    request:{
      use: function(){}
    },
    response:{
      use: function(){}
    }
  },
  defaults:{
    headers:{
      common:{}
    }
  },
  head: function(url,params) {
    return getActionOrm(url, params)
  },
  get: function(url,params) {
    return getActionOrm(url, params)
  },
  post: function(url,params) {
    return postActionOrm(url, params)
  },
  put: function(url,params) {
    return putActionOrm(url, params)
  },
  patch: function(url,params) {
    return patchActionOrm(url, params)
  },
  delete: function(url,params) {
    return deleteActionOrm(url, params)
  },
}