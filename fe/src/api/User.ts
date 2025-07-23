const url = process.env.NEXT_PUBLIC_BASE_URL;

export const checkEmail = async (email: string): Promise<CheckEmailRes> => {
  const data = await fetch(`${url}/api/users/check-email?email=${email}`, {
    method: "GET",
    credentials: "include",
  });
  if (!data.ok) throw new Error(data.statusText);
  return data.json();
};

export const login = async (email: string, password : string): Promise<LoginRes> => {
  const loginInfo = {
    email, 
    password
  }
  
  const data = await fetch(`${url}/api/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body : JSON.stringify(loginInfo)
  });

  console.log(loginInfo)
  console.log(data)
  if (!data.ok) throw new Error(data.statusText);
  return data.json();
};
