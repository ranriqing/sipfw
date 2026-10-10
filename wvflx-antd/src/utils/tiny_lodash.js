import get from 'lodash/get'
import set from 'lodash/set'
import defaultTo from 'lodash/defaultTo'

import trim from 'lodash/trim'
import omit from 'lodash/omit'
import uniq from 'lodash/uniq'
import random from 'lodash/random'
import toString from 'lodash/toString'
import isString from 'lodash/isString'

import now from 'lodash/now'
import reduce from 'lodash/reduce'
import pickBy from 'lodash/pickBy'
import cloneDeep from 'lodash/cloneDeep'
import forIn from 'lodash/forIn'
import uniqBy from 'lodash/uniqBy'
import sortBy from 'lodash/sortBy'
import orderBy from 'lodash/orderBy'
import sumBy from 'lodash/sumBy'
import countBy from 'lodash/countBy'
import groupBy from 'lodash/groupBy'

import has from 'lodash/has'
import some from 'lodash/some'
import first from 'lodash/first'
import every from 'lodash/every'
import union from 'lodash/union'
import camelCase from 'lodash/camelCase'
import capitalize from 'lodash/capitalize'
import upperFirst from 'lodash/upperFirst'
import intersection from 'lodash/intersection'

import isNil from 'lodash/isNil'
import isNaN from 'lodash/isNaN'
import isArray from 'lodash/isArray'
import isEqual from 'lodash/isEqual'
import isEmpty from 'lodash/isEmpty'
import isNumber from 'lodash/isNumber'
import isFunction from 'lodash/isFunction'
import isPlainObject from 'lodash/isPlainObject'

export default {
  get, set, defaultTo,
  trim, omit, uniq, random, toString, isString,
  sumBy, orderBy, countBy, groupBy,
  now, reduce, pickBy, cloneDeep, forIn, uniqBy, sortBy,
  has, first, some, every, union, camelCase, capitalize, upperFirst, intersection,
  isNaN, isNil, isArray, isEqual, isEmpty, isNumber,
  isFunction, isPlainObject,
}