<template xmlns:v-slot="http://www.w3.org/1999/XSL/Transform">
  <div>
    <el-row>
      <el-col>

        <el-form
            ref="SlsForm"
            :model="form"
            :rules="rule"
            size="small"
            :disabled="disabled"
            label-width="180px"
            label-position="right"
        >
          <el-form-item :label="$t('accessId')" prop="configuration.host">
            <el-input :placeholder="$t('enter_the_accessId')" v-model="form.configuration.accessId" autocomplete="off"/>
          </el-form-item>

          <el-form-item :label="$t('accessKey')" prop="configuration.port">
            <dePwd :placeholder="$t('enter_the_accessKey')" v-model="form.configuration.accessKey"/>
          </el-form-item>

          <el-form-item :label="$t('project')" prop="configuration.dataBase">
            <el-input :placeholder="$t('enter_the_project')" v-model="form.configuration.project" autocomplete="off"/>
          </el-form-item>

          <el-form-item :label="$t('host')" prop="configuration.username">
            <el-input :placeholder="$t('enter_the_entry_of_service')" v-model="form.configuration.host" autocomplete="off"/>
          </el-form-item>

          <el-form-item class="schema-label" :label="$t('logStore')">
            <template slot="label">
              {{ $t("logStore") }}
              <el-button type="text" icon="el-icon-plus" size="small" @click="getLogStore()">{{ $t('get_log_store') }}</el-button>
            </template>
            <el-select v-model="form.configuration.logStore" filterable
                       :placeholder="$t('please_select')"
                       class="de-select">
              <el-option v-for="item in logStores" :key="item" :label="item" :value="item"/>
            </el-select>
          </el-form-item>

          <el-form-item class="schema-label" :label="$t('defaultTimeRange')">
            <el-select v-model="form.configuration.defaultTimeRange" filterable
                       :placeholder="$t('please_select')"
                       class="de-select">
              <el-option v-for="item in defaultTimeRanges" :key="item.key" :label="item.key" :value="item.value"/>
            </el-select>
          </el-form-item>

          <el-form-item :label="$t('topic')" prop="configuration.password">
            <el-input :placeholder="$t('input_a_topic')" v-model="form.configuration.topic" />
          </el-form-item>
        </el-form>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import messages from '@/de-base/lang/messages'
import dePwd from "./dePwd.vue";

export default {
  name: "sls",
  components: { dePwd },
  props: {
    method: String,
    request: {},
    response: {},
    editApiItem: {
      type: Boolean,
      default() {
        return false;
      }
    },
    showScript: {
      type: Boolean,
      default: true,
    },
    obj: {
      type: Object,
      default() {
        return {
          configuration: {
            initialPoolSize: 5,
            extraParams: '',
            minPoolSize: 5,
            maxPoolSize: 50,
            maxIdleTime: 30,
            acquireIncrement: 5,
            idleConnectionTestPeriod: 5,
            connectTimeout: 5,
            defaultTimeRange: 'today'
          },
          apiConfiguration: []
        }
      }
    },
  },
  data() {
    return {
      rule: {
        'configuration.accessId': [{required: true, message: this.$t('commons.required'), trigger: 'blur'}],
        'configuration.accessKey': [{required: true, message: this.$t('commons.required'), trigger: 'blur'}],
        'configuration.project': [{required: true, message: this.$t('commons.required'), trigger: 'blur'}],
        'configuration.host': [{required: true, message: this.$t('commons.required'), trigger: 'blur'}],
        'configuration.logStore': [{required: true, message: this.$t('commons.required'), trigger: 'blur'}],
        'configuration.timeFrame': [{required: true, message: this.$t('commons.required'), trigger: 'blur'}]
      },
      canEdit: false,
      originConfiguration: {},
      height: 500,
      disabledNext: false,
      logStores: [],
      defaultTimeRanges: [
        {key: this.$t('oneMinutes'), value: 'oneMinutes'},
        {key: this.$t('fiveMinutes'), value: 'fiveMinutes'},
        {key: this.$t('oneHour'), value: 'oneHour'},
        {key: this.$t('oneDay'), value: 'oneDay'},
        {key: this.$t('oneWeek'), value: 'oneWeek'},
        {key: this.$t('oneMonth'), value: 'oneMonth'},
        {key: this.$t('oneYear'), value: 'oneYear'},
        {key: this.$t('today'), value: 'today'},
        {key: this.$t('week'), value: 'week'},
        {key: this.$t('month'), value: 'month'},
        {key: this.$t('year'), value: 'year'}
      ],
    }
  },
  computed: {
    form() {
      return this.obj.form
    },
    disabled() {
      return this.obj.disabled
    }
  },
  created() {
    this.$emit('on-add-languages', messages)
  },
  watch: {},
  methods: {
    executeAxios(url, type, data, callBack) {
      const param = {
        url: url,
        type: type,
        data: data,
        callBack: callBack
      }
      this.$emit('execute-axios', param)
    },
    getLogStore() {
      this.$refs["SlsForm"].validate(valid => {
        if (valid) {
          const data = JSON.parse(JSON.stringify(this.form))
          data.configuration = JSON.stringify(data.configuration)
          // todo:写接口
          this.executeAxios('/datasource/getSchema', 'post', data, res => {
            this.logStores = res.data
          })
        } else {
          return false
        }
      })
    },
    validate() {
      let status = null;
      this.$refs["SlsForm"].validate((val) => {
        if (val) {
          status = true
        } else {
          status = false
        }
      })

      if (!this.form.configuration.logStore) {
        this.$message.error(this.$t('please_choose_log_store'))
        status = false
      }
      return status
    }
  }
}
</script>

<style scoped>
.ms-query {
  background: #409EFF;
  color: white;
  height: 18px;
  border-radius: 42%;
}

.ms-header {
  background: #409EFF;
  color: white;
  height: 18px;
  border-radius: 42%;
}

.request-tabs {
  margin: 20px;
  min-height: 200px;
}

.ms-el-link {
  float: right;
  margin-right: 45px;
}
</style>
