export interface LoginData {
  email: string;
  password: string;
}

export interface RegisterData {
  email: string;
  password: string;
  name: string;           // теперь обязательное
}

export interface User {
  id: string;
  email: string;
  name: string;           // тоже обязательное
  token: string;
}

// Внутренний тип для хранения в localStorage (включает пароль)
interface StoredUser extends User {
  password: string;
}

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

const AuthService = {
  async register(data: RegisterData): Promise<User> {
    console.log('Register called with:', data);
    await delay(1000);

    const existing = localStorage.getItem(`user_${data.email}`);
    if (existing) {
      throw new Error('User already exists');
    }

    const storedUser: StoredUser = {
      id: Math.random().toString(36).substr(2, 9),
      email: data.email,
      name: data.name,               // используем
      password: data.password,
      token: 'fake-jwt-token-' + Math.random()
    };

    localStorage.setItem(`user_${data.email}`, JSON.stringify(storedUser));

    const userWithoutPassword: User = {
      id: storedUser.id,
      email: storedUser.email,
      name: storedUser.name,
      token: storedUser.token,
    };
    localStorage.setItem('user', JSON.stringify(userWithoutPassword));

    return userWithoutPassword;
  },

  async login(data: LoginData): Promise<User> {
    console.log('Login called with:', data);
    await delay(1000);

    const storedUserStr = localStorage.getItem(`user_${data.email}`);
    if (!storedUserStr) {
      throw new Error('Invalid email or password');
    }

    const storedUser: StoredUser = JSON.parse(storedUserStr);

    if (storedUser.password !== data.password) {
      throw new Error('Invalid email or password');
    }

    const userWithoutPassword: User = {
      id: storedUser.id,
      email: storedUser.email,
      name: storedUser.name,
      token: storedUser.token,
    };
    localStorage.setItem('user', JSON.stringify(userWithoutPassword));

    return userWithoutPassword;
  },

  logout(): void {
    localStorage.removeItem('user');
  },

  getCurrentUser(): User | null {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      return JSON.parse(userStr);
    }
    return null;
  }
};

export default AuthService;