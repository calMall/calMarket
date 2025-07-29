import { getProductDetail } from "@/api/Product";
import { getReviewDetail } from "@/api/Review";
import ContainImage from "@/components/common/ContainImage";
import CustomLayout from "@/components/common/CustomLayout";
import ErrorComponent from "@/components/common/ErrorComponent";
import ReviewWriteContain from "@/components/review/ReviewWriteContain";
import { newImageSizing } from "@/utils/newImageSizing";
import Link from "next/link";

export default async function ReviewDetail({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  try {
    const data = await getReviewDetail(Number(id));
    console.log(data);
    return (
      <CustomLayout>
        <h2>商品はいかがでしたか？</h2>
      </CustomLayout>
    );
  } catch (e) {
    return <ErrorComponent />;
  }
}
