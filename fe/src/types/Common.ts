interface ResponseDTO {
  message: "success" | "fail";
}
interface Pagenation extends ResponseDTO {
  currentPage: number;
  hasNext: boolean;
  totalElements: number;
  totalPages: number;
}
