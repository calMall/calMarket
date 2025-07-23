interface CheckEmailRes extends ResponseDTO {
  available: boolean;
}
interface SignupRes extends ResponseDTO {
  cartItemCount: number;
  nickname: string;
}
interface SignupReq {
  email: string;
  password: string;
  nickname: string;
  birth: string;
}
