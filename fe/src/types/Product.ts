interface ProductDetailResponseDto extends ResponseDTO {
  product: {
    itemCode: string;
    itemName: string;
    itemCaption: string;
    catchcopy: string;
    score: number;
    reviewCount: number;
    price: number;
    imageUrls: string[];
  };
}
