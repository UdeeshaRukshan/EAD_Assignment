import axios, { AxiosInstance, AxiosResponse } from 'axios';
import { AuthResponse, LoginRequest, ChargingStation, CreateStationRequest, Booking, CreateBookingRequest, UpdateBookingRequest, CompleteBookingRequest } from '../types';

class ApiService {
  private api: AxiosInstance;
  private authToken: string | null = null;

  constructor() {
    this.api = axios.create({
      baseURL: 'https://localhost:7001/api',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Add request interceptor to include auth token
    this.api.interceptors.request.use(
      (config) => {
        if (this.authToken) {
          config.headers.Authorization = `Bearer ${this.authToken}`;
        }
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // Add response interceptor to handle errors
    this.api.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          // Token expired or invalid
          this.setAuthToken(null);
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  setAuthToken(token: string | null) {
    this.authToken = token;
  }

  // Auth endpoints
  async login(email: string, password: string): Promise<AuthResponse> {
    const response: AxiosResponse<AuthResponse> = await this.api.post('/auth/login', {
      email,
      password,
    });
    return response.data;
  }

  async register(data: {
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber: string;
    password: string;
    role: number;
  }): Promise<AuthResponse> {
    const response: AxiosResponse<AuthResponse> = await this.api.post('/auth/register', data);
    return response.data;
  }

  // Charging Stations endpoints
  async getChargingStations(): Promise<ChargingStation[]> {
    const response: AxiosResponse<ChargingStation[]> = await this.api.get('/chargingstations');
    return response.data;
  }

  async getChargingStation(id: string): Promise<ChargingStation> {
    const response: AxiosResponse<ChargingStation> = await this.api.get(`/chargingstations/${id}`);
    return response.data;
  }

  async createChargingStation(data: CreateStationRequest): Promise<ChargingStation> {
    const response: AxiosResponse<ChargingStation> = await this.api.post('/chargingstations', data);
    return response.data;
  }

  async updateChargingStation(id: string, data: Partial<CreateStationRequest>): Promise<ChargingStation> {
    const response: AxiosResponse<ChargingStation> = await this.api.put(`/chargingstations/${id}`, data);
    return response.data;
  }

  async deleteChargingStation(id: string): Promise<void> {
    await this.api.delete(`/chargingstations/${id}`);
  }

  async updateStationStatus(id: string, status: number): Promise<ChargingStation> {
    const response: AxiosResponse<ChargingStation> = await this.api.patch(`/chargingstations/${id}/status`, {
      status,
    });
    return response.data;
  }

  // Booking endpoints
  async getAllBookings(): Promise<Booking[]> {
    const response: AxiosResponse<Booking[]> = await this.api.get('/bookings');
    return response.data;
  }

  async getUserBookings(userId: string): Promise<Booking[]> {
    const response: AxiosResponse<Booking[]> = await this.api.get(`/bookings/user/${userId}`);
    return response.data;
  }

  async getStationBookings(stationId: string): Promise<Booking[]> {
    const response: AxiosResponse<Booking[]> = await this.api.get(`/bookings/station/${stationId}`);
    return response.data;
  }

  async getPendingBookings(): Promise<Booking[]> {
    const response: AxiosResponse<Booking[]> = await this.api.get('/bookings/pending');
    return response.data;
  }

  async getBooking(id: string): Promise<Booking> {
    const response: AxiosResponse<Booking> = await this.api.get(`/bookings/${id}`);
    return response.data;
  }

  async createBooking(data: CreateBookingRequest): Promise<Booking> {
    const response: AxiosResponse<Booking> = await this.api.post('/bookings', data);
    return response.data;
  }

  async updateBooking(id: string, data: UpdateBookingRequest): Promise<Booking> {
    const response: AxiosResponse<Booking> = await this.api.put(`/bookings/${id}`, data);
    return response.data;
  }

  async confirmBooking(id: string): Promise<void> {
    await this.api.patch(`/bookings/${id}/confirm`);
  }

  async cancelBooking(id: string): Promise<void> {
    await this.api.patch(`/bookings/${id}/cancel`);
  }

  async completeBooking(id: string, data: CompleteBookingRequest): Promise<void> {
    await this.api.patch(`/bookings/${id}/complete`, data);
  }
}

export const apiService = new ApiService();