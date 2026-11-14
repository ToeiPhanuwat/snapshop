import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { STATUS_LABEL } from "@/lib/format";
import type { ProductStatus } from "@/types/api";

const TONE: Record<ProductStatus, BadgeTone> = {
  OK: "ok",
  NOT_IN_LATEST: "info",
  TEST: "neutral",
  OUT_OF_STOCK: "neutral",
  REVIEW: "warn",
};

export function StatusBadge({ status }: { status: ProductStatus }) {
  return <Badge tone={TONE[status]}>{STATUS_LABEL[status]}</Badge>;
}
