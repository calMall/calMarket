interface ReviewRequestDto {
  itemCode: string;
  rating: number;
  title: string;
  comment: string;
  imageList: string[];
}
interface ReviewDTO {
  reviewId: number;
  rating: number;
  title: string;
  comment: string;
  imageList: string[];
}

interface ReviewDTOonProduct extends ReviewDTO {
  like: boolean;
  likeCount: number;
  userNickname: string;
  createdAt: string;
}
type RatingStats = [
  { score: 5; count: number },
  { score: 4; count: number },
  { score: 3; count: number },
  { score: 2; count: number },
  { score: 1; count: number }
];

interface ReviewListDTO extends Pagenation {
  myReview: ReviewDTOonProduct;
  ratingStats: RatingStats;
  reviews: ReviewDTOonProduct[];
}
interface Review {
  id: number;
  title: string;
  createdAt: string;
  score: number;
  content: string;
}
