import axios, { AxiosInstance, AxiosResponse } from "axios";
import {
  AuthResponse,
  LoginRequest,
  ChargingStation,
  CreateStationRequest,
  Booking,
  CreateBookingRequest,
  UpdateBookingRequest,
  CompleteBookingRequest,
  User,
} from "../types";

class ApiService {
  private api: AxiosInstance;
  private authToken: string | null = null;

  constructor() {
    this.api = axios.create({
      baseURL: "http://localhost:5105/api",
      timeout: 300000,
      headers: {
        "Content-Type": "application/json",
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
          localStorage.removeItem("token");
          localStorage.removeItem("user");
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
    const response: AxiosResponse<AuthResponse> = await this.api.post(
      "/auth/login",
      {
        email,
        password,
      }
    );
    return response.data;
  }

  async register(data: {
    firstName: string;
    lastName: string;
    nic: string;
    email: string;
    phoneNumber: string;
    password: string;
    role: number;
  }): Promise<AuthResponse> {
    const response: AxiosResponse<AuthResponse> = await this.api.post(
      "/auth/register",
      data
    );
    return response.data;
  }

  async getProfile(): Promise<any> {
    const response: AxiosResponse<any> = await this.api.get("/auth/profile");
    console.log(response.data);
    return response.data;
  }

  async updateProfile(
    nic: string,
    data: Partial<{
      firstName: string;
      lastName: string;
      email: string;
      phoneNumber: string;
      password: string;
    }>
  ): Promise<any> {
    const response: AxiosResponse<any> = await this.api.put(
      `/users/${nic}`,
      data
    );
    return response.data;
  }

  async activateUser(nic: string): Promise<void> {
    await this.api.patch(`/users/${nic}/activate`);
  }

  async deactivateUser(): Promise<void> {
    await this.api.patch("/auth/deactivate");
  }

  async deleteUser(nic: string): Promise<void> {
    await this.api.delete(`/users/${nic}`);
  }

  // Admin User Management endpoints
  async getAllUsers(): Promise<User[]> {
    try {
      const response: AxiosResponse<User[]> = await this.api.get("/auth/users");
      return response.data;
    } catch (error: any) {
      // Log detailed error for debugging
      console.error('getAllUsers API Error:', {
        status: error.response?.status,
        message: error.response?.data?.message || error.message,
        hasToken: !!this.authToken
      });
      throw error;
    }
  }

  async getUserById(id: string): Promise<User> {
    try {
      const response: AxiosResponse<User> = await this.api.get(
        `/auth/users/${id}`
      );
      return response.data;
    } catch (error: any) {
      console.error('getUserById API Error:', {
        status: error.response?.status,
        message: error.response?.data?.message || error.message,
        userId: id
      });
      throw error;
    }
  }

  async updateUser(
    id: string,
    data: {
      firstName: string;
      lastName: string;
      email: string;
      phoneNumber: string;
      role: number;
      nic?: string;
      password?: string;
    }
  ): Promise<User> {
    try {
      // Backend expects RegisterRequest format
      const requestBody = {
        nic: data.nic || "",
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        phoneNumber: data.phoneNumber,
        password: data.password || "",
        role: data.role
      };

      const response: AxiosResponse<User> = await this.api.put(
        `/auth/users/${id}`,
        requestBody
      );
      return response.data;
    } catch (error: any) {
      console.error('updateUser API Error:', {
        status: error.response?.status,
        message: error.response?.data?.message || error.message,
        userId: id,
        requestData: data
      });
      throw error;
    }
  }

  async deleteUserByAdmin(id: string): Promise<void> {
    try {
      await this.api.delete(`/auth/users/${id}`);
    } catch (error: any) {
      console.error('deleteUserByAdmin API Error:', {
        status: error.response?.status,
        message: error.response?.data?.message || error.message,
        userId: id
      });
      throw error;
    }
  }

  async reactivateUser(id: string): Promise<void> {
    try {
      await this.api.patch(`/auth/users/${id}/reactivate`);
    } catch (error: any) {
      console.error('reactivateUser API Error:', {
        status: error.response?.status,
        message: error.response?.data?.message || error.message,
        userId: id
      });
      throw error;
    }
  }

  async deactivateUserByAdmin(id: string): Promise<void> {
    try {
      console.log('Deactivating user:', {
        userId: id,
        endpoint: `/auth/users/${id}/deactivate`,
        fullUrl: `http://localhost:5105/api/auth/users/${id}/deactivate`
      });
      await this.api.patch(`/auth/users/${id}/deactivate`);
    } catch (error: any) {
      console.error('deactivateUserByAdmin API Error:', {
        status: error.response?.status,
        statusText: error.response?.statusText,
        message: error.response?.data?.message || error.message,
        userId: id,
        endpoint: `/auth/users/${id}/deactivate`,
        responseData: error.response?.data
      });
      throw error;
    }
  }

  async getChargingStations(): Promise<ChargingStation[]> {
    const response: AxiosResponse<ChargingStation[]> = await this.api.get(
      "/chargingstations"
    );
    return response.data;
  }

  async getChargingStation(id: string): Promise<ChargingStation> {
    const response: AxiosResponse<ChargingStation> = await this.api.get(
      `/chargingstations/${id}`
    );
    return response.data;
  }

  async createChargingStation(
    data: CreateStationRequest
  ): Promise<ChargingStation> {
    const response: AxiosResponse<ChargingStation> = await this.api.post(
      "/chargingstations",
      data
    );
    return response.data;
  }

  async patchChargingStation(
    id: string,
    data: Partial<CreateStationRequest>
  ): Promise<ChargingStation> {
    const response: AxiosResponse<ChargingStation> = await this.api.patch(
      `/chargingstations/${id}`,
      data
    );
    return response.data;
  }

  async deleteChargingStation(id: string): Promise<void> {
    await this.api.delete(`/chargingstations/${id}`);
  }

  async updateStationStatus(
    id: string,
    status: number
  ): Promise<ChargingStation> {
    const response: AxiosResponse<ChargingStation> = await this.api.patch(
      `/chargingstations/${id}/status`,
      {
        status,
      }
    );
    return response.data;
  }

  // Booking endpoints
  async getAllBookings(): Promise<Booking[]> {
    const response: AxiosResponse<Booking[]> = await this.api.get("/bookings");
    return response.data;
  }

  async getUserBookings(userId: string): Promise<Booking[]> {
    const response: AxiosResponse<Booking[]> = await this.api.get(
      `/bookings/user/${userId}`
    );
    return response.data;
  }

  async getStationBookings(stationId: string): Promise<Booking[]> {
    const response: AxiosResponse<Booking[]> = await this.api.get(
      `/bookings/station/${stationId}`
    );
    return response.data;
  }

  async getPendingBookings(): Promise<Booking[]> {
    const response: AxiosResponse<Booking[]> = await this.api.get(
      "/bookings/pending"
    );
    return response.data;
  }

  async getBooking(id: string): Promise<Booking> {
    const response: AxiosResponse<Booking> = await this.api.get(
      `/bookings/${id}`
    );
    return response.data;
  }

  async createBooking(data: CreateBookingRequest): Promise<Booking> {
    const response: AxiosResponse<Booking> = await this.api.post(
      "/bookings",
      data
    );
    return response.data;
  }

  async updateBooking(
    id: string,
    data: UpdateBookingRequest
  ): Promise<Booking> {
    const response: AxiosResponse<Booking> = await this.api.put(
      `/bookings/${id}`,
      data
    );
    return response.data;
  }

  async confirmBooking(id: string): Promise<void> {
    await this.api.patch(`/bookings/${id}/confirm`);
  }

  async cancelBooking(id: string): Promise<void> {
    await this.api.patch(`/bookings/${id}/cancel`);
  }

  async completeBooking(
    id: string,
    data: CompleteBookingRequest
  ): Promise<void> {
    await this.api.patch(`/bookings/${id}/complete`, data);
  }
}

export const apiService = new ApiService();
