const url = process.env.NEXT_PUBLIC_BASE_URL;

export const checkEmail = async (email: string): Promise<CheckEmailRes> => {
  const data = await fetch(`${url}/api/users/check-email?email=${email}`, {
    method: "GET",
    credentials: "include",
  });
  if (!data.ok) throw new Error(data.statusText);
  return data.json();
};
