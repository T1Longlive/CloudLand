import Vue from 'vue'
import VueRouter from 'vue-router'
import Frontend from '../views/frontend/MainView'
import Backend from '../views/backend/MainView'
import Message from "element-ui/lib/message";
import axiosInstance from "@/request/axiosInstance";
import {createEmptyUser, getCachedLoginState, getStoredToken, setCachedLoginState} from "@/utils/auth";

Vue.use(VueRouter)

const routes = [
    //前端商城页面路由
    {
        path: '/',
        component: Frontend,
        props: true,
        children: [
            {
                path: "",
                component: () => import('../views/frontend/HomeView'),
                meta: {
                    title: '云用地'
                }
            },
            {
                path: "land",
                component: () => import('../views/frontend/LandView'),
            },
            {
                path: "landInfo/:id",
                component: () => import('../views/frontend/LandInfoView'),
            },
            {
                path: "product",
                component: () => import('../views/frontend/ProductView'),
            },
            {
                path: "productInfo/:id",
                component: () => import('../views/frontend/ProductInfoView'),
            },
            {
                path: "ForgetPassword/:id",
                component: () => import('../views/frontend/ForgetPassword'),
            },
            {
                path: "user",
                component: () => import('../views/frontend/UserView'),
                children: [
                    {
                        path: "",
                        component: () => import('../views/frontend/userInfo/UserMsgView'),
                    },
                    {
                        path: "password",
                        component: () => import('../views/frontend/userInfo/UserPassword'),
                    },
                    {
                        path: "contact",
                        component: () => import('../views/frontend/userInfo/UserContactView'),
                    },
                    {
                        path: "myOrder",
                        component: () => import('../views/frontend/userInfo/MyOrder'),
                    },
                    {
                        path: "myTrolley",
                        component: () => import('../views/frontend/userInfo/MyTrolley'),
                    },
                ]
            }
        ]
    },

    //后端管理页面路由
    {
        path: '/backend',
        component: Backend,
        meta: {
            title: '云用地后台'
        },
        children: [
            {
                path: "",
                component: () => import('../views/backend/HomeView'),
            },
            {
                path: "customerA",
                component: () => import('../views/backend/Customer_aView'),
            },
            {
                path: "customerB",
                component: () => import('../views/backend/Customer_bView'),
            },
            {
                path: "employee",
                component: () => import('../views/backend/EmployeeView'),
            },
            {
                path: "product",
                component: () => import('../views/backend/ProductView'),
            },
            {
                path: "land",
                component: () => import('../views/backend/LandView'),
            },
            {
                path: "orderLand",
                component: () => import('../views/backend/LandOrderView'),
            },
            {
                path: "orderProduct",
                component: () => import('../views/backend/ProductOrderView'),
            },
            {
                path: "pushMsg",
                component: () => import('../views/backend/PushMsg'),
            }
        ]
    },
]

const router = new VueRouter({
    mode: "history",
    base: process.env.BASE_URL,
    routes,
    scrollBehavior(to, from, savedPosition) {
        // 前端页面均为整页滚动布局，路由切换后回到顶部（浏览器前进/后退恢复原位置）
        if (savedPosition) {
            return savedPosition;
        }
        return {x: 0, y: 0};
    }
});
router.afterEach((to) => {
    document.title = to.meta.title || '云用地'
})

/**
 * 校验当前登录态（供路由守卫使用）
 * 性能：原先每次导航都 POST /user/login 校验，导航密集时请求放大；
 * 现在配合 auth.js 的登录态缓存（TTL 5 分钟，登录成功/登出/清 token 时失效），
 * 缓存命中时不发请求。
 */
async function isUserLoggedIn() {
    const token = getStoredToken();
    if (!token) {
        return false;
    }
    const cached = getCachedLoginState();
    if (cached !== null) {
        return cached;
    }
    const config = {
        headers: {
            'token': token,
            'remember': 'false', // 添加自定义请求头
            'frond': 'true', // 添加自定义请求头
        }
    };
    let {data: res} = await axiosInstance.post('/user/login', createEmptyUser(), config)
    const loggedIn = res.code === 20005;
    setCachedLoginState(loggedIn);
    return loggedIn;
}

// 需要登录才能访问的后台页面（其余后台页面由各页面内部的 openCheck 处理）
const PROTECTED_BACKEND_PATHS = ['/backend/land', '/backend/customerA', '/backend/customerB', '/backend/employee'];

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
    const isLoggedIn = await isUserLoggedIn();
    if (PROTECTED_BACKEND_PATHS.includes(to.path) && !isLoggedIn) {
        Message.warning('未登录！')
        next('/');
    } else if (to.path.startsWith('/user') && !isLoggedIn) {
        Message.warning('请先在首页右上方登录!')
        next('/');
    } else {
        next();
    }
})
export default router;
