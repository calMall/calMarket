interface CheckEmailRes extends ResponseDTO {
  available: boolean;
}
interface LoginRes extends ResponseDTO {
  cartItemCount : number;
  nickname : string;
}

// interface CheckEmailRes extends ResponseDTO {
//   message: "success" | "fail";
//   available: boolean;
// }

