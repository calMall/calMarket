interface rakutenApiItem {
  Item: {
    catchcopy: "楽天生活応援米";
    genreId: "201184";
    imageFlag: 1 | 0;
    itemCaption: string;
    itemCode: string;
    itemName: string;
    itemPrice: string;
    itemUrl: string;
    mediumImageUrls: { imageUrl: string }[];
    rank: number;
    reviewAverage: string;
    reviewCount: 3029;
    shopCode: string;
    shopName: string;
    shopUrl: string;
  };
}

interface RakutenAPIResponse {
  Items: rakutenApiItem[];
  count: number;
  hits: number;
  last: number;
  page: number;
}
