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
          :label="$t('plugin_view_3d_bar.inner_angle')"
          class="form-item form-item-slider"
        >
          <el-slider
            v-model="sizeForm.innerAngle"
            show-input
            :show-input-controls="false"
            input-size="mini"
            :min="0"
            :max="45"
            @change="changeBarSizeCase()"
          />
        </el-form-item>

        <el-form-item
          :label="$t('plugin_view_3d_bar.outer_angle')"
          class="form-item form-item-slider"
        >
          <el-slider
            v-model="sizeForm.outerAngle"
            show-input
            :show-input-controls="false"
            input-size="mini"
            :min="-45"
            :max="45"
            @change="changeBarSizeCase()"
          />
        </el-form-item>

        <el-form-item
          :label="$t('plugin_view_3d_bar.depth')"
          class="form-item form-item-slider"
        >
          <el-slider
            v-model="sizeForm.depth"
            show-input
            :show-input-controls="false"
            input-size="mini"
            :min="20"
            :max="100"
            @change="changeBarSizeCase()"
          />
        </el-form-item>

        <el-form-item
          :label="$t('plugin_view_3d_bar.column_depth')"
          class="form-item form-item-slider"
        >
          <el-slider
            v-model="sizeForm.columnDepth"
            show-input
            :show-input-controls="false"
            input-size="mini"
            :min="20"
            :max="100"
            @change="changeBarSizeCase()"
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
        this.initData()
      }
    },
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
    changeBarSizeCase() {
      if (this.sizeForm.gaugeMax <= this.sizeForm.gaugeMin) {
        this.$message.error(this.$t('chart.max_more_than_mix'))
        return
      }
      this.$emit('onSizeChange', this.sizeForm)
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
