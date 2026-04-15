import Vue from 'vue'
import VueRouter from 'vue-router'
import Frontend from '../views/frontend/MainView'
import Backend from '../views/backend/MainView'
import axiosInstance from "@/request/axiosInstance";

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
});
router.afterEach((to) => {
    document.title = to.meta.title || '云用地'
})

async function isUserLoggedIn() {
    const isToken1 = sessionStorage.getItem("token") !== undefined && sessionStorage.getItem("token") !== null
    const isToken2 = localStorage.getItem("token") !== undefined && localStorage.getItem("token") !== null
    let token = null
    if (isToken1 || isToken2) {
        if (isToken1) {
            token = sessionStorage.getItem("token")
        } else {
            token = localStorage.getItem("token")
        }
    } else {
        return false
    }
    const user = {
        id: null,
        username: null,
        password: null,
        phone: null,
        age: null,
        address: null,
        img: null,
        status: 1,
        detailedAddress: null,
        power: 0,
        mail: null
    }
    const config = {
        headers: {
            'token': token,
            'remember': 'false', // 添加自定义请求头
            'frond': 'true', // 添加自定义请求头
        }
    };
    let {data: res} = await axiosInstance.post('/user/login', user, config)
    return res.code === 20005;
}

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
    // 在这里可以编写拦截逻辑，例如检查用户是否有权限访问该路由
    // 如果要允许跳转，调用 next()；如果要拦截跳转，调用 next(false) 或者 next('/other-route') 来重定向到其他路由
    // 示例：检查用户是否登录，如果未登录，跳转到登录页
    const isLoggedIn = await isUserLoggedIn();
    if ((to.path === '/backend/land' ||
        to.path === '/backend/customerA' ||
        to.path === '/backend/customerB' ||
        to.path === '/backend/employee') && !isLoggedIn) {
        alert('未登录！')
        next('/');
    } else if (to.path.startsWith('/user') && !isLoggedIn) {
        alert('请先在首页右上方登录!')
        next('/');
    } else {
        if ((from.path === "/" && sessionStorage.getItem("main") === "0") || (from.path === "/" && to.path === "/")) {

        } else {
            sessionStorage.setItem("replace", "1");
        }
        if (to.path === "/") {
            sessionStorage.setItem("main", "1");
        } else {
            sessionStorage.setItem("main", "0");
        }
        next();
    }
})
export default router;
