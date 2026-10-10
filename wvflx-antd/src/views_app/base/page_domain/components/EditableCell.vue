<template>
  <div>
    <a-form-item
      :validateStatus="status"
      :help="help">
      <a-input
        v-model="record[fieldName]"
        @change="onChange"
        :placeholder="placeholder">
      </a-input>
    </a-form-item>
  </div>
</template>

<script>
  export default {
    name: "EditableCell",
    components:{
    },
    props: {
      record: {
        type: Object,
        required: true,
      },
      fieldName: {
        type: String,
        required: true,
      },
      validator: {
        type: Function,
        default: null
      },
      placeholder: {
        type: String,
        default: '',
      },
    },
    data () {
      return {
        status: null,
        help: null,
      }
    },
    methods: {
      onChange(e) {
        let that = this
        let value = e.target.value
        if(that.validator){
          that.validator(null, value, function(rst){
            if(rst){
              that.status = 'error'
              that.help = rst
            }else{
              that.status = null
              that.help = null
            }
          })
        }
      }
    },
  }
</script>

