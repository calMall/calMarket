interface CheckEmailRes extends ResponseDTO {
  available: boolean;
}
interface LoginRes extends ResponseDTO {
  cartItemCount: number;
  nickname: string;
}
interface SignupReq {
  email: string;
  password: string;
  nickname: string;
  birth: string;
}

interface UserStore {
  cartItemCount: number;
  nickname: string;
}

interface MyinfoDTO extends ResponseDTO {
  point: number;
  orders: SimpleOrder[];
  reviews: Review[];
}

interface SimpleOrder {
  id: number;
  imageUrl: string;
}
