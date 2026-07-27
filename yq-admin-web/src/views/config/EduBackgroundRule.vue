<template>
  <section v-loading="loading" class="edu-rule-editor" aria-label="学历规则编辑器">
    <div class="editor-heading">
      <div>
        <p class="section-kicker">EDUCATION LEVELS</p>
        <h2>学历等级配置</h2>
      </div>
      <div class="heading-actions">
        <span class="item-count"><b>{{ items.length }}</b> 个学历等级</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openAdd">
          新增等级
        </el-button>
      </div>
    </div>

    <div v-if="!loading && items.length === 0" class="empty-ledger">
      <el-empty description="尚未配置学历等级，点击「新增等级」开始" :image-size="100" />
    </div>

    <ol v-else class="level-ledger">
      <li
        v-for="(item, index) in items"
        :key="item.id"
        :class="['ledger-row', { inactive: item.status !== 'ACTIVE' }]"
      >
        <span class="ordinal">{{ String(index + 1).padStart(2, '0') }}</span>
        <div class="level-info">
          <b>{{ item.description || item.itemKey }}</b>
          <code>{{ item.itemKey }}</code>
        </div>
        <div class="level-value">
          <span class="value-chip">{{ item.itemValue }}</span>
          <small>{{ valueTypeLabel(item.valueType) }}</small>
        </div>
        <span :class="['st', item.status === 'ACTIVE' ? 'on' : 'off']">
          <i></i>{{ item.status === 'ACTIVE' ? '启用' : '停用' }}
        </span>
        <div class="row-actions">
          <el-button type="text" size="mini" icon="el-icon-edit" @click="openEdit(item)">
            编辑
          </el-button>
          <el-button
            type="text"
            size="mini"
            class="danger-link"
            icon="el-icon-delete"
            @click="handleDelete(item)"
          >
            删除
          </el-button>
        </div>
      </li>
    </ol>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      :title="dialogMode === 'add' ? '新增学历等级' : '编辑学历等级'"
      :visible.sync="dialogVisible"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="form" :model="form" :rules="formRules" label-width="96px" size="small">
        <el-form-item label="等级编码" prop="itemKey">
          <el-input
            v-model="form.itemKey"
            placeholder="如 HIGH_SCHOOL、BACHELOR"
            :disabled="dialogMode === 'edit'"
          />
        </el-form-item>
        <el-form-item label="等级名称" prop="description">
          <el-input v-model="form.description" placeholder="如 高中、大专、本科" />
        </el-form-item>
        <el-form-item label="等级值" prop="itemValue">
          <el-input v-model="form.itemValue" placeholder="用于业务逻辑的标识值" />
        </el-form-item>
        <el-form-item label="值类型" prop="valueType">
          <el-select v-model="form.valueType" placeholder="选择值类型" style="width: 100%">
            <el-option label="字符串 (STRING)" value="STRING" />
            <el-option label="整数列表 (INTEGER_LIST)" value="INTEGER_LIST" />
            <el-option label="布尔 (BOOLEAN)" value="BOOLEAN" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitting" @click="submitForm">
          {{ dialogMode === 'add' ? '确认新增' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script>
import {
  listConfigItems,
  addConfigItem,
  updateConfigItem,
  deleteConfigItem
} from '@/api/sysConfig'

const VALUE_TYPE_MAP = {
  STRING: '字符串',
  INTEGER_LIST: '整数列表',
  BOOLEAN: '布尔'
}

export default {
  name: 'EduBackgroundRule',
  props: {
    typeId: { type: Number, required: true }
  },
  data() {
    return {
      loading: false,
      submitting: false,
      items: [],
      dialogVisible: false,
      dialogMode: 'add',
      editingId: null,
      form: this.emptyForm(),
      formRules: {
        itemKey: [
          { required: true, message: '请输入等级编码', trigger: 'blur' },
          { pattern: /^[A-Z][A-Z0-9_]{1,63}$/, message: '2~64位大写字母、数字或下划线', trigger: 'blur' }
        ],
        description: [
          { required: true, message: '请输入等级名称', trigger: 'blur' }
        ],
        itemValue: [
          { required: true, message: '请输入等级值', trigger: 'blur' }
        ],
        valueType: [
          { required: true, message: '请选择值类型', trigger: 'change' }
        ],
        sortOrder: [
          { required: true, message: '请输入排序号', trigger: 'blur' }
        ],
        status: [
          { required: true, message: '请选择状态', trigger: 'change' }
        ]
      }
    }
  },
  watch: {
    typeId: {
      immediate: true,
      handler(val) {
        if (val) this.loadItems()
      }
    }
  },
  methods: {
    emptyForm() {
      return {
        itemKey: '',
        description: '',
        itemValue: '',
        valueType: 'STRING',
        sortOrder: 0,
        status: 'ACTIVE'
      }
    },
    valueTypeLabel(type) {
      return VALUE_TYPE_MAP[type] || type
    },
    async loadItems() {
      this.loading = true
      try {
        const res = await listConfigItems(this.typeId)
        this.items = Array.isArray(res.data) ? res.data : []
      } catch (e) {
        this.items = []
      } finally {
        this.loading = false
      }
    },
    openAdd() {
      this.dialogMode = 'add'
      this.editingId = null
      this.form = this.emptyForm()
      this.form.sortOrder = this.items.length
      this.dialogVisible = true
    },
    openEdit(item) {
      this.dialogMode = 'edit'
      this.editingId = item.id
      this.form = {
        itemKey: item.itemKey,
        description: item.description || '',
        itemValue: item.itemValue,
        valueType: item.valueType,
        sortOrder: item.sortOrder,
        status: item.status
      }
      this.dialogVisible = true
    },
    resetForm() {
      this.$refs.form && this.$refs.form.resetFields()
    },
    submitForm() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.submitting = true
        const payload = { ...this.form, typeId: this.typeId }
        try {
          if (this.dialogMode === 'add') {
            await addConfigItem(payload)
            this.$message.success('学历等级已新增')
          } else {
            await updateConfigItem(this.editingId, payload)
            this.$message.success('学历等级已更新')
          }
          this.dialogVisible = false
          await this.loadItems()
        } catch (e) {
          // 错误由请求层统一展示
        } finally {
          this.submitting = false
        }
      })
    },
    handleDelete(item) {
      const name = item.description || item.itemKey
      this.$confirm(`确定删除学历等级「${name}」吗？删除后不可恢复。`, '删除确认', {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteConfigItem(item.id)
          this.$message.success('已删除')
          await this.loadItems()
        } catch (e) {
          // 错误由请求层统一展示
        }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.edu-rule-editor {
  background: var(--card);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.editor-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 22px 24px 18px;
  border-bottom: 1px solid var(--line);
}
.section-kicker {
  margin-bottom: 5px;
  color: var(--brass-ink);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .18em;
}
.editor-heading h2 {
  color: var(--ink);
  font-family: var(--font-display);
  font-size: 19px;
}
.heading-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}
.item-count {
  color: var(--ink-3);
  font-size: 12px;
}
.item-count b {
  font-family: var(--font-mono);
  font-size: 16px;
  color: var(--jade-deep);
}

.empty-ledger {
  display: grid;
  place-items: center;
  min-height: 240px;
}

.level-ledger {
  list-style: none;
  margin: 0;
  padding: 0;
}
.ledger-row {
  display: grid;
  grid-template-columns: 44px minmax(180px, 1.4fr) minmax(120px, 1fr) 72px 120px;
  align-items: center;
  gap: 16px;
  padding: 15px 24px;
  border-bottom: 1px solid var(--line-soft);
  transition: background-color .15s ease;
}
.ledger-row:last-child { border-bottom: 0; }
.ledger-row:hover { background: #FAFCF8; }
.ledger-row.inactive { opacity: .55; }

.ordinal {
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 700;
  color: var(--brass-ink);
  background: var(--brass-soft);
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 6px;
}
.level-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}
.level-info b {
  color: var(--ink);
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.level-info code {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--jade-deep);
}
.level-value {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.value-chip {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 500;
  color: var(--ink-2);
  background: #F3F6F0;
  border: 1px solid var(--line-soft);
  padding: 2px 10px;
  border-radius: 4px;
  max-width: 160px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.level-value small {
  color: var(--ink-3);
  font-size: 11px;
}
.row-actions {
  display: flex;
  gap: 4px;
  justify-content: flex-end;
}

@media (max-width: 860px) {
  .ledger-row {
    grid-template-columns: 36px 1fr auto;
    grid-template-rows: auto auto;
    gap: 8px 12px;
    padding: 14px 16px;
  }
  .ordinal { width: 28px; height: 28px; font-size: 11px; grid-row: 1 / span 2; }
  .level-info { grid-column: 2; }
  .level-value { grid-column: 2; grid-row: 2; flex-direction: row; align-items: center; gap: 8px; }
  .ledger-row .st { grid-column: 3; grid-row: 1; justify-self: end; }
  .row-actions { grid-column: 3; grid-row: 2; justify-self: end; }
  .editor-heading { flex-direction: column; align-items: flex-start; gap: 12px; }
}
</style>
