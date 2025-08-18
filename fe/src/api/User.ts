const url = process.env.NEXT_PUBLIC_BASE_URL;

export const checkEmail = async (email: string): Promise<CheckEmailRes> => {
  const data = await fetch(`${url}/users/check-email?email=${email}`, {
    method: "GET",
    credentials: "include",
  });
  if (!data.ok) throw new Error(data.statusText);
  return data.json();
};

export const login = async (
  email: string,
  password: string
): Promise<LoginRes> => {
  const loginInfo = {
    email,
    password,
  };

  const data = await fetch(`${url}/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(loginInfo),
  });
  if (!data.ok) throw new Error(data.statusText);
  return data.json();
};

export const signup = async (signupData: SignupReq): Promise<ResponseDTO> => {
  const data = await fetch(`${url}/users`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(signupData),
  });

  if (!data.ok) throw new Error(data.statusText);
  return data.json();
};

export const logout = async (): Promise<ResponseDTO> => {
  const data = await fetch(`${url}/logout`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
  });
  if (!data.ok) throw new Error(data.statusText);
  return data.json();
};
// 自分の情報照会
export const getMyInfo = async (): Promise<MyinfoDTO> => {
  const data = await fetch(`${url}/users/me`, {
    method: "GET",
    credentials: "include",
  });

  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status; // 상태 코드 추가
    throw error;
  }
  return data.json();
};
export const addeAddress = async (
  address: deliveryAddressDetail
): Promise<MyinfoDTO> => {
  const data = await fetch(`${url}/users/addresses`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify(address),
  });

  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    throw error;
  }
  return data.json();
};
export const deleteAddress = async (
  address: deliveryAddressDetail
): Promise<MyinfoDTO> => {
  const data = await fetch(`${url}/users/addresses/delete`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify(address),
  });

  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    throw error;
  }
  return data.json();
};
