import { getProductDetail } from "@/api/Product";
import { getReviewDetail } from "@/api/Review";
import ContainImage from "@/components/common/ContainImage";
import CustomLayout from "@/components/common/CustomLayout";
import ErrorComponent from "@/components/common/ErrorComponent";
import Star from "@/components/product/Star";
import Link from "next/link";

export default async function ReviewDetail({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  try {
    const data = await getReviewDetail(Number(id));
    // const product = await getProductDetail(data);
    console.log(data);
    return (
      // <div className="review-item">
      //   {/* 商品情報 */}
      //   <div className="product-info grid-2" style={{ alignItems: "center", gap: "1rem" }}>
      //     <ContainImage
      //       src={product.imageUrl}
      //       alt="商品画像"
      //       width={120}
      //       height={120}
      //       className="product-image"
      //     />
      //     <div className="product-title">{product.name}</div>
      //   </div>

      //   {/* レビュー内容 */}
      //   <div className="simple-review-contain">
      //     <div className="review-title-in-product">{data.title}</div>
      //     <Star score={data.score} />
      //     <div className="date-font">
      //       {new Date(data.createdAt).toLocaleDateString("ja-JP", {
      //         year: "numeric",
      //         month: "long",
      //         day: "numeric",
      //       })}
      //     </div>
      //     <div className="comment">{data.comment}</div>
      //   </div>

      //   {/* 編集・削除ボタン */}
      //   <div className="je" style={{ display: "flex", gap: "1rem", marginTop: "1rem" }}>
      //     <Link href={`/review/edit/${data.id}`}>
      //       <button className="edit review-post-btn">編集</button>
      //     </Link>
      //     <form action={`/review/delete/${data.id}`} method="POST">
      //       <button type="submit" className="delete review-post-btn">削除</button>
      //     </form>
      //   </div>
      // </div>
      <CustomLayout>
        <div></div>
      </CustomLayout>
    );
  } catch (e) {
    console.log(e);
    return <ErrorComponent />;
  }
}
