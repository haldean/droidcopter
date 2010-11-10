#include <stdio.h>

int main(int argc, char* argv[]) {
  int i;
  long j = 0;
  
  for (i = getchar(); i != EOF; i = getchar()) {
    printf("%d ", i * 4);
    j++;

    if (j % 4 == 0) {
      printf("\n");
    }

    if (j == 1000) break;
  }

  return 0;
}
