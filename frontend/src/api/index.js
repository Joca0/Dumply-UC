import axios from 'axios';
import {toast} from "sonner";

const api = axios.create({
  baseURL: window.location.protocol + '//' + window.location.hostname + (window.location.port ? ':' + window.location.port : '') + '/api',
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('@dumply:token');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
}, (error) => {
  return Promise.reject(error);
});

api.interceptors.response.use(
  res => res,
  error => {
    if (error.response) {
      const { status, data } = error.response;

      if (status === 401) {
        window.dispatchEvent(
          new CustomEvent('auth:logout', {
            detail: { reason: 'expired' }
          })
        );
        toast.error(typeof data === 'string' ? data : 'Não autorizado ou sessão expirada');
      }

      if (status === 409) {
        toast.warning(typeof data === 'string' ? data : 'Conflito de dados');
      }

      if (status === 500) {
        toast.error(typeof data === 'string' ? data : 'Erro interno do servidor');
      }
    } else {
      toast.error('Erro de conexão com o servidor');
    }

    return Promise.reject(error);
  }
);

export const login = (credentials) => api.post('/auth/login', credentials);
export const verify2FA = (email, code) => api.post(`/auth/2fa/verify?email=${email}&code=${code}`);
export const setup2FA = () => api.post('/auth/2fa/setup');
export const confirm2FA = (code) => api.post(`/auth/2fa/confirm?code=${code}`);
export const requestDisable2FA = () => api.post('/auth/2fa/disable/request');
export const confirmDisable2FA = (code) => api.post(`/auth/2fa/disable/confirm?code=${code}`);
export const profile = () => api.get('/auth/me');
export const logout = () => api.post('/auth/logout');
export const forgotPassword = (email) => api.post('/auth/forgot-password', { email });
export const resetPassword = (token, newPassword) => api.post('/auth/reset-password', { token, newPassword });
export const changePassword = (data) => api.post('/auth/change-password', data);

export const register = (data) => api.post('/companies/signup', data);
export const completeWelcome = () => api.patch('/auth/complete-welcome');

export const getDashboardStats = () => api.get('/dashboard/stats');

export const autocompleteEquipments = (search) => api.get(`/equipments/autocomplete?q=${search}`);
export const autocompleteCustomers = (search) => api.get(`/customers/autocomplete?q=${search}`);
export const autocompleteDrivers = (search) => api.get(`/users/drivers/autocomplete?q=${search}`);

export const getEquipments = (page = 0, size = 10, search = '') => api.get(`/equipments?page=${page}&size=${size}&search=${search}`);
export const getEquipment = (id) => api.get(`/equipments/${id}`);
export const createEquipment = (data) => api.post('/equipments', data);
export const updateEquipment = (id, data) => api.put(`/equipments/${id}`, data);
export const deleteEquipment = (id) => api.delete(`/equipments/${id}`);

export const getCustomers = (page = 0, size = 10, search = '') => api.get(`/customers?page=${page}&size=${size}&search=${search}`);
export const getCustomer = (id) => api.get(`/customers/${id}`);
export const createCustomer = (data) => api.post('/customers', data);
export const updateCustomer = (id, data) => api.put(`/customers/${id}`, data);
export const deleteCustomer = (id) => api.delete(`/customers/${id}`);

export const createDriver = (data) => api.post('/users/driver', data);
export const createManager = (data) => api.post('/users/manager', data);
export const getDrivers = () => api.get('/users/drivers');
export const getManagers = () => api.get('/users/managers');
export const getUser = (id) => api.get(`/users/${id}`);
export const updateUser = (id, data) => api.put(`/users/${id}`, data);
export const deleteUser = (id) => api.delete(`/users/${id}`);

export const getAssignedRentals = (page = 0, size = 10) => {
  return api.get(`/rentals/assigned?page=${page}&size=${size}`);
}

export const getRentals = (page = 0, size = 10, filters = {}) => {
  const params = new URLSearchParams({ page, size, ...filters });
  return api.get(`/rentals?${params.toString()}`);
}
export const getScheduledRentals = (page = 0, size = 10, filters = {}) => {
  const params = new URLSearchParams({ page, size, ...filters });
  return api.get(`/rentals/scheduled?${params.toString()}`);
}
export const activateRental = (id) => api.post(`/rentals/${id}/activate`);
export const assignDriver = (id, driverId) => api.put(`/rentals/${id}/driver`, driverId, {
  headers: {
    'Content-Type': 'application/json'
  }
});
export const getRental = (id) => api.get(`/rentals/${id}`);
export const getActiveRentals = () => api.get('/rentals/active');
export const createRental = (data) => api.post('/rentals', data);
export const updateRental = (id, data) => api.put(`/rentals/${id}`, data)
export const returnRental = (id) => api.post(`/rentals/${id}/return`);
//export const deleteRental = (id) => api.delete(`/rentals/${id}`);

export const getInvoices = (page = 0, size = 10, filters = {}) => {
  const params = new URLSearchParams({ page, size, ...filters });
  return api.get(`/invoices?${params.toString()}`);
}
export const getInvoiceStats = () => api.get('/invoices/stats');
export const getInvoice = (id) => api.get(`/invoices/${id}`);
export const createInvoice = (data) => api.post('/invoices', data);
export const getUninvoicedRentals = (customerId) => api.get(`/invoices/uninvoiced/${customerId}`);
export const updateInvoiceStatus = (id, status) => api.put(`/invoices/${id}/status`, status, {
  headers: {
    'Content-Type': 'application/json'
  }
});

export default api;