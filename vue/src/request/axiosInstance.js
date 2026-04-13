import axios from "axios";
import {APP_CONFIG} from "@/config/app";

const axiosInstance = axios.create({
    baseURL: APP_CONFIG.apiBaseUrl,
    timeout: 600000,
});

export default axiosInstance;

