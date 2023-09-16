<template>
  <div style="width: 100%">
    <el-col>
      <el-form
        ref="sizeFormBar"
        :model="sizeForm"
        label-width="80px"
        size="mini"
      >
        <!--pie-begin-->
        <el-form-item
          :label="$t('plugin_view_3d_ring.inner_size')"
          class="form-item form-item-slider"
        >
          <el-slider
            v-model="sizeForm.innerSize"
            show-input
            :show-input-controls="false"
            input-size="mini"
            :min="0"
            :max="200"
            @change="changeBarSizeCase('pieInnerRadius')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('plugin_view_3d_ring.depth')"
          class="form-item form-item-slider"
        >
          <el-slider
            v-model="sizeForm.ringDepth"
            show-input
            :show-input-controls="false"
            input-size="mini"
            :min="0"
            :max="100"
            @change="changeBarSizeCase('pieOuterRadius')"
          />
        </el-form-item>
        <!--pie-end-->
      </el-form>
    </el-col>
  </div>
</template>

<script>
import { DEFAULT_SIZE } from '@/utils/map'

export default {
  name: 'SizeSelector',
  props: {
    param: {
      type: Object,
      required: true
    },
    chart: {
      type: Object,
      required: true
    },
    propertyInner: {
      type: Array,
      required: false,
      default: function() {
        return []
      }
    },
    quotaFields: {
      type: Array,
      required: true
    }
  },
  data() {
    return {
      sizeForm: JSON.parse(JSON.stringify(DEFAULT_SIZE)),
      fontSize: [],
      minField: {},
      maxField: {},
      liquidMaxField: {},
      quotaData: [],
    }
  },
  watch: {
    'chart': {
      handler: function() {
        // this.initField()
        this.initData()
      }
    },
    // 'quotaFields': function() {
    //   this.initField()
    // }
  },
  mounted() {
    this.init()
    this.initData()
  },
  methods: {
    initData() {
      const chart = JSON.parse(JSON.stringify(this.chart))
      if (chart.customAttr) {
        let customAttr = null
        if (Object.prototype.toString.call(chart.customAttr) === '[object Object]') {
          customAttr = JSON.parse(JSON.stringify(chart.customAttr))
        } else {
          customAttr = JSON.parse(chart.customAttr)
        }
      }
    },
    init() {
      const arr = []
      for (let i = 10; i <= 60; i = i + 2) {
        arr.push({
          name: i + '',
          value: i + ''
        })
      }
      this.fontSize = arr
    },
    changeBarSizeCase(modifyName) {
      this.sizeForm['modifyName'] = modifyName
      if (this.sizeForm.gaugeMax <= this.sizeForm.gaugeMin) {
        this.$message.error(this.$t('chart.max_more_than_mix'))
        return
      }
      this.$emit('onSizeChange', this.sizeForm)
    },
    showProperty(property) {
      return this.propertyInner.includes(property)
    },
    getQuotaField(id) {
      if (!id) {
        return {}
      }
      const fields = this.quotaData.filter(ele => {
        return ele.id === id
      })
      if (fields.length === 0) {
        return {}
      } else {
        return fields[0]
      }
    },
  }
}
</script>

<style scoped>
  .form-item-slider ::v-deep .el-form-item__label {
    font-size: 12px;
    line-height: 38px;
  }

  .form-item ::v-deep .el-form-item__label {
    font-size: 12px;
  }

  span {
    font-size: 12px
  }

  .divider-style ::v-deep .el-divider__text {
    color: #606266;
    font-size: 12px;
    font-weight: 400;
    padding: 0 10px;
  }
  .form-flex >>> .el-form-item__content {
    display: flex;
  }
</style>
