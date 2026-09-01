import axiosInstance from "./axiosInstance";
import router from "@/router";
import Message from "element-ui/lib/message";
import {clearAuthState, getStoredToken, hasPersistentToken, persistToken} from "@/utils/auth";

let installed = false;

/**
 * 设置Axios拦截器
 *
 * 重要说明：
 * 1. HTTP状态码（error.response.status）：由服务器HTTP层返回，如401/403/500
 * 2. 业务状态码（response.data.code）：由业务逻辑返回，如10001/20005等
 *
 * 这两者是不同的概念，不要混淆！
 * - HTTP状态码：在此拦截器中处理（网络层错误）
 * - 业务状态码：在具体的业务代码中处理（业务逻辑错误）
 */
export const setupInterceptors = () => {
    if (installed) {
        return;
    }
    installed = true;

    // ==================== 请求拦截器 ====================
    axiosInstance.interceptors.request.use(config => {
        const token = getStoredToken();
        if (token) {
            config.headers.token = token;
        }
        return config;
    });

    // ==================== 响应拦截器 ====================
    axiosInstance.interceptors.response.use(
        (response) => {
            // 处理token刷新
            const updatedToken = response.headers["updatedtoken"];
            if (updatedToken) {
                persistToken(updatedToken, hasPersistentToken());
            }
            return response;
        },
        (error) => {
            // ==================== HTTP状态码处理 ====================
            // 注意：这里处理的是HTTP层面的错误，不是业务状态码

            if (error.response && error.response.status === 401) {
                // HTTP 401 Unauthorized - 未认证
                console.log('HTTP 401: 未认证，清除登录状态');
                clearAuthState();
                router.push({path: "/"}).then(() => null);
            }

            if (error.response && error.response.status === 402) {
                // HTTP 402 Payment Required - 权限变更
                console.log('HTTP 402: 账号权限发生变更');
                Message.warning("账号权限发生变更！");
                clearAuthState();
                router.push({path: "/"}).then(() => null);
            }

            if (error.response && error.response.status === 403) {
                // HTTP 403 Forbidden - 账号被禁用 或 无权限访问后台接口
                // 通过 X-Forbidden-Reason 响应头区分：status=禁用（清登录态），power=权限不足（保留登录态）
                const reason = error.response.headers["x-forbidden-reason"];
                if (reason === "power") {
                    console.log('HTTP 403: 无权限访问该功能');
                    Message.error("您没有权限访问该功能！");
                } else {
                    console.log('HTTP 403: 账号已被禁用');
                    Message.error("账号已被禁用！");
                    clearAuthState();
                    router.push({path: "/"}).then(() => null);
                }
            }

            if (error.response && error.response.status === 500) {
                // HTTP 500 Internal Server Error - 服务器错误
                console.error('HTTP 500: 服务器内部错误', error);
            }

            return Promise.reject(error);
        }
    );
};
