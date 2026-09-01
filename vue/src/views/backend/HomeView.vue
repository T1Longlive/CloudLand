<template>
  <div class="home-dashboard">
    <!-- 顶部欢迎横幅（沿用品牌背景图） -->
    <div class="hero">
      <div class="hero-text">
        <p class="hero-title">您好{{ nameSuffix }}，欢迎使用云用地后台系统！</p>
        <p class="hero-sub">{{ today }}</p>
      </div>
    </div>

    <div class="dashboard-body" v-loading="loading">
      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stat-row">
        <el-col v-for="card in visibleCards" :key="card.label" :span="cardSpan">
          <router-link :to="card.path" class="stat-card">
            <div class="stat-icon"><i :class="card.icon"></i></div>
            <div class="stat-info">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </router-link>
        </el-col>
      </el-row>

      <!-- 最新订单 -->
      <div class="recent-card">
        <div class="recent-header">
          <span class="recent-title">最新订单</span>
          <router-link to="/backend/orderLand" class="recent-more">查看全部</router-link>
        </div>
        <el-table :data="recentOrders" style="width: 100%" empty-text="暂无订单数据">
          <el-table-column prop="id" label="订单号" width="90"></el-table-column>
          <el-table-column prop="productName" label="商品名称" min-width="140"
                           show-overflow-tooltip></el-table-column>
          <el-table-column label="类型" width="100">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.num===-1" type="success" size="mini">用地</el-tag>
              <el-tag v-else size="mini">普通商品</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额(元)" width="100">
            <template slot-scope="scope">
              <span>{{ scope.row.num === -1 ? scope.row.price : scope.row.num * scope.row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column label="支付状态" width="100">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.status===0" type="warning" size="mini">未支付</el-tag>
              <el-tag v-else-if="scope.row.status===1" type="success" size="mini">已支付</el-tag>
              <el-tag v-else type="info" size="mini">已退款</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="下单时间" min-width="150"></el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import axiosInstance from "@/request/axiosInstance";

export default {
  name: "home",
  props: ['parentData'], // 接收父组件传递的当前用户
  data() {
    return {
      loading: false,
      stats: {
        users: null,
        lands: null,
        products: null,
        landOrders: null,
        productOrders: null
      },
      recentOrders: []
    }
  },
  computed: {
    isAdmin() {
      return this.parentData && this.parentData.power === 2;
    },
    cardSpan() {
      return this.isAdmin ? 6 : 8;
    },
    nameSuffix() {
      return this.parentData && this.parentData.username ? '，' + this.parentData.username : '';
    },
    today() {
      const d = new Date();
      const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()];
      return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${week}`;
    },
    orderTotal() {
      const a = this.stats.landOrders;
      const b = this.stats.productOrders;
      if (a === null && b === null) {
        return '—';
      }
      return (a || 0) + (b || 0);
    },
    visibleCards() {
      const cards = [];
      if (this.isAdmin) {
        cards.push({
          label: '普通用户',
          value: this.formatCount(this.stats.users),
          icon: 'el-icon-user-solid',
          path: '/backend/customerA'
        });
      }
      cards.push({
        label: '用地总数',
        value: this.formatCount(this.stats.lands),
        icon: 'el-icon-s-grid',
        path: '/backend/land'
      });
      cards.push({
        label: '产品总数',
        value: this.formatCount(this.stats.products),
        icon: 'el-icon-apple',
        path: '/backend/product'
      });
      cards.push({
        label: '订单总数',
        value: this.orderTotal,
        icon: 'el-icon-s-claim',
        path: '/backend/orderLand'
      });
      return cards;
    }
  },
  watch: {
    // 登录态确认后（parentData 有值）再拉取统计数据，避免未登录时的无效请求
    parentData: {
      immediate: true,
      handler(val) {
        if (val && val.id !== null && val.id !== undefined) {
          this.loadDashboard();
        }
      }
    }
  },
  methods: {
    formatCount(v) {
      return v === null ? '—' : v;
    },
    // 分页查询接口取 total（pageSize=1 只为拿总数）
    async fetchTotal(url, bodyKey, payload) {
      const formData = new FormData();
      formData.append(bodyKey, JSON.stringify(payload));
      formData.append('pageNum', 1);
      formData.append('pageSize', 1);
      const {data: res} = await axiosInstance.post(url, formData);
      return res.data ? res.data.total : null;
    },
    // 订单查询：num=-1 为用地订单，null 为产品订单（与后台订单页一致）
    async fetchOrders(num) {
      const order = {
        id: null, pId: null, num: num, uId: null, createTime: null, payTime: null,
        status: null, img: null, price: null, del: null, username: null, phone: null
      };
      // 员工（power=1）仅可见自己的订单
      if (this.parentData.power === 1) {
        order.uId = this.parentData.id;
      }
      const formData = new FormData();
      formData.append('order', JSON.stringify(order));
      formData.append('pageNum', 1);
      formData.append('pageSize', 5);
      const {data: res} = await axiosInstance.post('/order/page', formData);
      if (!res.data) {
        return {total: null, records: []};
      }
      const records = (res.data.records || []).map(r => ({
        ...r,
        createTime: r.createTime ? r.createTime.replace(/T/g, ' ') : ''
      }));
      return {total: res.data.total, records};
    },
    async loadDashboard() {
      this.loading = true;
      const p = this.parentData;
      const isEmployee = p.power === 1;
      // 单项失败不影响其它统计（显示为 —）
      const safe = promise => promise.catch(() => null);
      const [users, lands, products, landRes, productRes] = await Promise.all([
        this.isAdmin ? safe(this.fetchTotal('/user/page', 'user', {
          id: null, username: null, password: null, phone: null, age: null,
          address: null, img: null, status: null, detailedAddress: null, power: 0, debt: null
        })) : Promise.resolve(null),
        safe(this.fetchTotal('/land/page', 'land', {aId: isEmployee ? p.id : null})),
        safe(this.fetchTotal('/product/page', 'product', {aId: isEmployee ? p.id : null})),
        safe(this.fetchOrders(-1)),
        safe(this.fetchOrders(null))
      ]);
      this.stats.users = users;
      this.stats.lands = lands;
      this.stats.products = products;
      this.stats.landOrders = landRes ? landRes.total : null;
      this.stats.productOrders = productRes ? productRes.total : null;
      // 合并两类订单按时间倒序取最近 5 条
      const recent = [
        ...(landRes ? landRes.records : []),
        ...(productRes ? productRes.records : [])
      ];
      recent.sort((a, b) => (b.createTime || '').localeCompare(a.createTime || ''));
      this.recentOrders = recent.slice(0, 5);
      this.loading = false;
    }
  }
}
</script>

<style scoped>
.home-dashboard {
  min-height: 100%;
  background-color: #f5f7f6;
}

/* 顶部欢迎横幅 */
.hero {
  height: 240px;
  background: linear-gradient(rgba(0, 0, 0, 0.55), rgba(0, 0, 0, 0.55)), url("../../assets/images/home-bg.jpg") center/cover no-repeat;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-text {
  text-align: center;
  color: #fff;
  animation: fadeIn .5s linear;
}

.hero-title {
  font-size: 3rem;
  font-weight: bolder;
  margin: 0;
}

.hero-sub {
  font-size: 1.5rem;
  margin-top: 1rem;
  opacity: .85;
}

@keyframes fadeIn {
  0% {
    opacity: 0;
  }
  100% {
    opacity: 1;
  }
}

.dashboard-body {
  padding: 2rem;
}

/* 统计卡片 */
.stat-row {
  margin-bottom: 2rem;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  background: #fff;
  border-radius: 8px;
  padding: 2rem;
  box-shadow: var(--shadow-card);
  transition: transform .2s, box-shadow .2s;
  color: inherit;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.stat-icon {
  flex: 0 0 auto;
  width: 5.6rem;
  height: 5.6rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.6rem;
  color: #fff;
  background: var(--color-primary);
}

.stat-value {
  font-size: 2.8rem;
  font-weight: bolder;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 1.4rem;
  color: var(--color-text-secondary);
  margin-top: .4rem;
}

/* 最新订单卡片 */
.recent-card {
  background: #fff;
  border-radius: 8px;
  padding: 2rem;
  box-shadow: var(--shadow-card);
}

.recent-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.recent-title {
  font-size: 1.8rem;
  font-weight: bolder;
  color: var(--color-text-primary);
}

.recent-more {
  font-size: 1.4rem;
  color: var(--color-primary);
}

.recent-more:hover {
  color: var(--color-primary-dark);
}
</style>
