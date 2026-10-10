<template>
  <div>
    <span v-if="cptScOption" class="sc-elem">
      <a-tag v-if="!cptScOption.tagStyle" size="small" class="sc-tag" style="border: none;">
        {{ cptScOption.title }}
      </a-tag>
      <a-tag v-else-if="cptScOption.tagStyle==='default'" size="small" class="sc-tag" style="border: none;">
        {{ cptScOption.title }}
      </a-tag>
      <a-tag v-else :color="cptScOption.tagStyle" size="small" class="sc-tag">
        {{ cptScOption.title }}
      </a-tag>
    </span>
    <template v-else-if="cptScOptionArray">
      <span v-for="(scOption, index) in cptScOptionArray" class="sc-elem" :key="index">
        <a-tag v-if="!scOption.tagStyle" size="small" class="sc-tag" style="border: none;">
          {{ scOption.title }}
        </a-tag>
        <a-tag v-else-if="scOption.tagStyle==='default'" size="small" class="sc-tag" style="border: none;">
          {{ scOption.title }}
        </a-tag>
        <a-tag v-else :color="scOption.tagStyle" size="small" class="sc-tag">
          {{ scOption.title }}
        </a-tag>
      </span>
    </template>
  </div>

</template>

<script>
  import ScElement from '@/store/models/ScElement'

  export default {
    name: 'ScElem',
    props: {
      scKey: {
        type: String,
        required: true
      },
      id: {
        type: Number,
        required: false,
        default: function(){
          return undefined
        }
      },
      idArray: {
        type: Array,
        required: false,
        default: function(){
          return undefined
        }
      },
    },
    computed: {
      cptScOption: function() {
        let that = this
        if(that.id){
          let scQuery = ScElement.query()
            .where(item => item.scConfigKey === that.scKey).where(item => item.id == that.id)
          return scQuery.orderBy('seqOrder').orderBy('id').first()
        }else{
          return undefined
        }
      },
      cptScOptionArray: function() {
        let that = this
        let scElemArray = []
        if(that.idArray){
          that.idArray.forEach( id => {
            let scQuery = ScElement.query()
              .where(item => item.scConfigKey === that.scKey)
              .where(item => item.id === id)
            let scElem = scQuery.orderBy('seqOrder').orderBy('id').first()
            if(scElem){ scElemArray.push(scElem) }
          })
        }
        return scElemArray
      }
    }
  }
</script>

<style lang="scss" scoped>
  .sc-elem{
    .sc-tag{
      line-height: 16px;
    }
  }
</style>